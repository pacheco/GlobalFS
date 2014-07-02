#!/usr/bin/env python
import flask
import sys
from time import sleep
from kazoo.client import KazooClient

app = flask.Flask(__name__)
zk = KazooClient()

storage = dict()

@app.route('/')
def index():
    return "Server running\n"

@app.route('/<string:key>', methods = ['GET'])
def get(key):
    try:
        val = storage[key]
        sleep(0.001 * len(val))
        return val
    except KeyError:
        return flask.make_response("", 404)
    

@app.route('/<string:key>', methods = ['PUT'])
def put(key):
    val = flask.request.get_data()
    sleep(0.001 * len(val))
    if key in storage:
        return flask.make_response("", 409)
    storage[key] = val
    return ""# "Put for key %s\n" % (key)
    
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
    app.run(debug = False, host="0.0.0.0", port = int(sys.argv[1]), threaded = True)
