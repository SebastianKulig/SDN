import csv
import os
import sys
from subprocess import Popen
from time import sleep, time

from mininet.cli import CLI
from mininet.log import setLogLevel
from mininet.log import setLogLevel, info
from mininet.net import Mininet
from mininet.node import RemoteController, OVSSwitch
from mininet.topo import Topo
from mininet.util import dumpNodeConnections


class CustomTopo(Topo):

    def build(self):
        # Create hosts
        h1 = self.addHost('h1')
        h2 = self.addHost('h2')
        h3 = self.addHost('h3')
        h4 = self.addHost('h4')
        server = self.addHost('server')

        # Create a switch
        s1 = self.addSwitch('s1')

        # Add links
        self.addLink(h1, s1)
        self.addLink(h2, s1)
        self.addLink(h3, s1)
        self.addLink(h4, s1)
        self.addLink(server, s1)


def start_monitor():
    info('*** Start monitor\n')
    cmd = "bwm-ng -o csv -T rate -C ',' > tmp.txt &"
    Popen(cmd, shell=True).wait()


def start_attack(net):
    info('*** Start attack\n')
    h1 = net.get('h1')
    server = net.get('server')
    h1.cmd("hping3 --flood --udp -p 80 {} &".format(server.IP()))


def stop_attack():
    info('*** Stop attack\n')
    cmd = "killall hping3"
    Popen(cmd, shell=True).wait()


def stop_monitor():
    info('*** Stop monitor\n')
    cmd = "killall bwm-ng ; sed -i '1i timestamp,interface,bytes_out_s,bytes_in_s,bytes_total_s,bytes_in,bytes_out,packets_out_s,packets_in_s,packets_total_s,packets_in,packets_out,errors_out_s,errors_in_s,errors_in,errors_out' tmp.txt"
    Popen(cmd, shell=True).wait()


def run():
    # Create an instance of our topology
    topo = CustomTopo()
    # Create a network based on the topology using OVS and controlled by a remote controller.
    net = Mininet(topo=topo, switch=OVSSwitch, autoSetMacs=True)
    # Start the network
    net.start()
    # Start http server
    net.get('server').cmd('python -m SimpleHTTPServer 80 &')
    start_monitor()
    sleep(5)
    start_attack(net)
    sleep(5)
    stop_attack()
    sleep(5)
    stop_monitor()
    net.stop()


if __name__ == '__main__':
    setLogLevel('info')
    run()

# Allows the file to be imported using 'mn --custom <filename> --topo custom
topos = {
    'custom': CustomTopo
}
