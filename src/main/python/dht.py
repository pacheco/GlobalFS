#!/usr/bin/env python
import flask
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
        return storage[key]
    except KeyError:
        return flask.make_response("", 404)
    

@app.route('/<string:key>', methods = ['PUT'])
def put(key):
    if key in storage:
        return flask.make_response("", 409)
    storage[key] = flask.request.get_data()
    return ""# "Put for key %s\n" % (key)
    
@app.route('/<string:key>', methods = ['DELETE'])
def delele(key):
    try:
        del storage[key]
    except:
        return flask.make_response("", 404)
    return ""

@zk.ChildrenWatch("/paxosfs/dht")
def node_watch(event):
    print event
    
# @app.route('/key')
# def move():
#     return "MOVE\n"
    

if __name__ == '__main__':
    print "Running storage node..."
    # zk.start()
    # zk.create("/paxosfs/dht/1", ephemeral=True)
    # zk.ensure_path("/paxosfs/dht")
    app.run(debug = True)
