import expcontrol
import socket

if __name__ == '__main__':
    hostname = socket.gethostname()
    if hostname == 'node1':
        expcontrol.init('examplecfg.json')
    else:
        expcontrol.init('examplecfg.json', hostname)
    expcontrol.barrier(1)

