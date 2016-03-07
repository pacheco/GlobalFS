package main

import (
	"bufio"
	"bytes"
	"fmt"
	"hash/fnv"
	"io/ioutil"
	"net/http"
	"os"
	"strconv"
	"strings"
)

// #cgo LDFLAGS: -lrocksdb -lstdc++ -lm -lz -lbz2 -lsnappy
import "github.com/tecbot/gorocksdb"

type HostPort struct {
	Host string
	Port int
}

func (hp *HostPort) AsString() string {
	return hp.Host + ":" + strconv.FormatInt(int64(hp.Port), 10)
}

func (hp *HostPort) PortString() string {
	return strconv.FormatInt(int64(hp.Port), 10)
}

// ------------ LevelDB Storage ------------------

// From reading levigo's and levelDB's docs, it looks like the DB is
// safe for concurrent use by multiple goroutines without extra
// synchronization.
var opts = gorocksdb.NewDefaultOptions()
var storeDB *gorocksdb.DB
var readOpt = gorocksdb.NewDefaultReadOptions()
var writeOpt = gorocksdb.NewDefaultWriteOptions()

func storageInit(path string) {
	//opts.IncreaseParallelism(2)
	opts.OptimizeForPointLookup(128) // 128mb cache size
	//opts.SetWriteBufferSize(4*1024*1024) // default 4mb
	opts.SetCreateIfMissing(true)
	opts.SetCompression(gorocksdb.NoCompression)
	opts.SetDisableDataSync(true)
	opts.SetUseFsync(false) // true:fsync, false:fdatasync
	db, err := gorocksdb.OpenDb(opts, path)
	if err != nil {
		panic("error opening DB at " + path + ": " + err.Error())
	}
	storeDB = db
}

func storageClose() {
	opts.Destroy()
	readOpt.Destroy()
	writeOpt.Destroy()
	storeDB.Close()
}

func storageGet(key []byte) ([]byte, error) {
	// TODO: check into using Get() -> Slice
	data, err := storeDB.GetBytes(readOpt, key)
	if err != nil {
		return nil, err
	} else {
		return data, nil
	}
}

func storagePut(key []byte, value []byte) error {
	err := storeDB.Put(writeOpt, key, value)
	return err
}

// ----------- End LevelDB Storage ---------------

// replication config
var selfId int
var replication int
var replicas = make([]HostPort, 0)

// From the docs, this can be shared by all goroutines.
var sharedClient = &http.Client{}

func parseConfig(file string) {
	f, err := os.Open(file)
	if err != nil {
		panic("ERROR: opening config file")
	}
	// read file line by line
	scanner := bufio.NewScanner(bufio.NewReader(f))
	scanner.Split(bufio.ScanLines)
	for {
		if !scanner.Scan() {
			if scanner.Err() != nil {
				continue
			} else {
				break
			}
		}
		line := scanner.Text()
		values := strings.Fields(line)
		// skip empty lines
		if len(values) == 0 {
			continue
		}
		switch values[0] {
		case "replication":
			rep, err := strconv.ParseInt(values[2], 0, 32)
			replication = int(rep)
			if err != nil {
				panic("ERROR: parsing config file")
			}
		default:
			port, err := strconv.ParseInt(values[1], 0, 32)
			if err != nil {
				panic("ERROR: parsing config file")
			}
			// FIXME: lua dht does HTTP on config port + 1
			replicas = append(replicas, HostPort{values[0], int(port) + 1})
		}
	}
}

func storagesForKey(keyhash uint32) []HostPort {
	n := len(replicas)
	pos := int(keyhash) % n
	switch replication {
	case 0:
		panic("replication should be > 0")
	case 1:
		return []HostPort{replicas[pos]}
	case 2:
		return []HostPort{replicas[pos], replicas[(pos+1)%n]}
	case 3:
		return []HostPort{replicas[pos], replicas[(pos+1)%n], replicas[(pos+2)%n]}
	default:
		panic("replication number not supported")
	}
}

// handle GET for a key. Fetches the key from the correct replica if
// the key is not local.
func handleGet(w http.ResponseWriter, key string, keyhash uint32) {
	storages := storagesForKey(keyhash)
	status := 200
	isLocal := false
	val := []byte{}

	for _, st := range storages {
		if st == replicas[selfId] {
			isLocal = true
		}
	}

	// handle get locally if possible
	if isLocal {
		var err error
		val, err = storageGet([]byte(key))
		if err != nil {
			status = 500
		} else if val == nil {
			status = 404
		}
	} else {
		// remote get
		c := sharedClient
		st := storages[0]
		url := "http://" + st.AsString() + "/" + key
		resp, err := c.Get(url)
		defer resp.Body.Close()
		if err != nil {
			w.WriteHeader(500)
			return
		}
		status = resp.StatusCode
		val, err = ioutil.ReadAll(resp.Body)
		if err != nil {
			w.WriteHeader(500)
			return
		}
	}

	// reply
	if status == 200 {
		w.Write(val)
	} else {
		w.WriteHeader(status)
	}
}

// used as a goroutine for async remote put (POST)
func remotePut(putUrl string, data []byte, statusc chan int, errc chan error) {
	req, err := http.NewRequest("POST", putUrl, bytes.NewReader(data))
	if err != nil {
		errc <- err
		return
	}
	c := sharedClient
	resp, err := c.Do(req)
	resp.Body.Close()
	if err != nil {
		errc <- err
		return
	}
	statusc <- resp.StatusCode
}

// handle PUT for a key. Will replicate by sending a POST for the key
// on other replicas, given the "replication" factor (0 means a single
// replica, 1 means 2 and so on).
func handlePut(w http.ResponseWriter, r *http.Request, key string, keyhash uint32) {
	data, err := ioutil.ReadAll(r.Body)
	if err != nil {
		w.WriteHeader(500)
		return
	}
	storages := storagesForKey(keyhash)
	isLocal := false
	status := 200

	rc := make(chan int, len(storages))
	errc := make(chan error, len(storages))
	for _, st := range storages {
		if st == replicas[selfId] {
			// local put
			isLocal = true
		} else {
			// remote put
			go remotePut("http://"+st.AsString()+"/"+key, data, rc, errc)
		}
	}
	// store locally
	if isLocal {
		val, err := storageGet([]byte(key))
		if val != nil {
			status = 409
		} else if err != nil {
			status = 500
		} else {
			err := storagePut([]byte(key), data)
			if err != nil {
				status = 500
			}
		}
	}
	// return early on error
	if status != 200 {
		w.WriteHeader(status)
		return
	}
	// check remotes are ok
	remotes := len(storages)
	if isLocal {
		remotes--
	}
	for i := 0; i < remotes; i++ {
		rStatus := 500
		var err error = nil
		select {
		case err = <-errc:
		case rStatus = <-rc:
		}
		// return early on error
		if err != nil || rStatus != 200 {
			w.WriteHeader(rStatus)
			return
		}
	}
	w.WriteHeader(200)
	return
}

// Replication is done by sending POST to other replicas. Handles the POST by storing locally only.
func handlePutLocal(w http.ResponseWriter, r *http.Request, key string, keyhash uint32) {
	data, err := ioutil.ReadAll(r.Body)
	if err != nil {
		w.WriteHeader(500)
		return
	}

	val, err := storageGet([]byte(key))
	if val != nil { // key exists
		w.WriteHeader(409)
	} else if err != nil { // db error
		w.WriteHeader(500)
	} else { // ok, write to db
		err := storagePut([]byte(key), data)
		if err != nil { // error storing
			w.WriteHeader(500)
		}
	}
}

func handler(w http.ResponseWriter, r *http.Request) {
	key := r.URL.Path[1:]
	// hash key
	h := fnv.New32a()
	h.Write([]byte(key))
	keyhash := h.Sum32()
	// find storage
	switch r.Method {
	case "GET":
		// fmt.Println("GET", key)
		handleGet(w, key, keyhash)
	case "PUT":
		// fmt.Println("PUT", key)
		handlePut(w, r, key, keyhash)
	case "POST":
		// fmt.Println("POST", key)
		handlePutLocal(w, r, key, keyhash)
	default:
		fmt.Println("UNSUPPORTED")
		w.WriteHeader(405)
	}
}

func main() {
	if len(os.Args) != 4 {
		panic("usage: kvstore <cfgfile> <id> <dbpath>")
	}
	id, err := strconv.ParseInt(os.Args[2], 0, 32)
	if err != nil {
		panic("self id must be an integer")
	}
	selfId = int(id)
	// read config
	parseConfig(os.Args[1])
	// initDB
	storageInit(os.Args[3])

	// serve
	hostString := replicas[selfId].AsString()
	fmt.Println("replication = ", replication)
	fmt.Println("Serving HTTP at:", hostString)
	http.HandleFunc("/", handler)
	http.ListenAndServe(":"+replicas[selfId].PortString(), nil)

	// closeDB
	storageClose()
}
