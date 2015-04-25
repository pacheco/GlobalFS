#!/usr/bin/env python
import flask
import sys
from time import sleep
#from kazoo.client import KazooClient
from gevent.wsgi import WSGIServer


app = flask.Flask(__name__)
#zk = KazooClient()

storage = dict()

@app.route('/')
def index():
    result = """
<p>KEYS STORED: %s</p>
<p>VALUES STORED IN KBYTES: %s</p>
""" % (len(storage), sum([sys.getsizeof(v) for v in storage.values()]) / 1024.0)
    return result

@app.route('/<string:key>', methods = ['GET'])
def get(key):
    try:
        val = storage[key]
        #sleep(0.001 * len(val))
        return val
    except KeyError:
        return flask.make_response("", 404)
    
@app.route('/<string:key>', methods = ['PUT'])
def put(key):
    val = flask.request.get_data()
    #sleep(0.001 * len(val))
    if key in storage:
        return flask.make_response("", 409)
    storage[key] = val
    return flask.make_response("", 200)# "Put for key %s\n" % (key)
    
@app.route('/<string:key>', methods = ['DELETE'])
def delele(key):
    try:
        del storage[key]
    except:
        return flask.make_response("", 404)
    return ""

# @zk.ChildrenWatch("/paxosfs/dht")
# def node_watch(event):
#     print event
    
# @app.route('/key')
# def move():
#     return "MOVE\n"
    

if __name__ == '__main__':
    print "Running storage node..."
    # zk.start()
    # zk.create("/paxosfs/dht/1", ephemeral=True)
    # zk.ensure_path("/paxosfs/dht")
    http_server = WSGIServer(('', int(sys.argv[1])), app)
    http_server.serve_forever()
    #app.run(debug = False, host="0.0.0.0", port = int(sys.argv[1]), threaded = True)
