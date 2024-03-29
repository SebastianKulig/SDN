import csv
import os
import sys
from subprocess import Popen
import time

from mininet.cli import CLI
from mininet.log import setLogLevel
from mininet.log import setLogLevel, info
from mininet.net import Mininet
from mininet.node import RemoteController, OVSSwitch
from mininet.topo import Topo
from mininet.util import dumpNodeConnections
from mininet.link import TCLink

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
        linkOpts = dict(bw=10, delay='5ms', max_queue_size=1000, use_htb=True)
        linkOptsToServer = dict(bw=10, delay='5ms', max_queue_size=4000, use_htb=True)
        self.addLink(h1, s1, **linkOpts)
        self.addLink(h2, s1, **linkOpts)
        self.addLink(h3, s1, **linkOpts)
        self.addLink(h4, s1, **linkOpts)
        self.addLink(server, s1, **linkOptsToServer)


def start_monitor():
    info('*** Start monitor\n')
    cmd = "bwm-ng -o csv -T rate -C ',' > tmp.txt &"
    Popen(cmd, shell=True).wait()


def start_attack(net):
    info('*** Start attack\n')
    h1 = net.get('h1')
    server = net.get('server')
#    h1.cmd("hping3 --flood -p 80 {} &".format(server.IP()))
#    h1.cmd("hping3 -1 -d 1000000000 {} &".format(server.IP()))
    h1.cmd("hping3 -q -n -d 120 -S -p 80 --flood {} &".format(server.IP()))

def start_normal_traffic(net):
   info('*** Start normal traffic ***\n')
   h2 = net.get('h2')
   server = net.get('server')
   h2.cmd('hping3 -1 -d 1000 {} &'.format(server.IP()))
   
   h3 = net.get('h3')
   h3.cmd('hping3 -1 -d 1000 {} &'.format(server.IP()))

   h4 = net.get('h4')
   h4.cmd('hping3 -1 -d 1000 {} &'.format(server.IP()))

def stop_attack():
    info('*** Stop attack\n')
    cmd = "killall hping3"
    Popen(cmd, shell=True).wait()


def stop_monitor():
    info('*** Stop monitor\n')
    cmd = "killall bwm-ng ; sed -i '1i timestamp,interface,bytes_out_s,bytes_in_s,bytes_total_s,bytes_in,bytes_out,packets_out_s,packets_in_s,packets_total_s,packets_in,packets_out,errors_out_s,errors_in_s,errors_in,errors_out' tmp.txt"
    Popen(cmd, shell=True).wait()


def run():
    controler = RemoteController('controler', ip='127.0.0.1', port=6653)
    # Create an instance of our topology
    topo = CustomTopo()
    # Create a network based on the topology using OVS and controlled by a remote controller.
    net = Mininet(topo=topo, switch=OVSSwitch, autoSetMacs=True, controller=controler, link = TCLink)

    # Start the network
    net.start()

    # Start http server
    net.get('server').cmd('python -m SimpleHTTPServer 80 &')

    # Start generating traffic
    start_normal_traffic(net)
    time.sleep(2)    
    start_monitor()
    time.sleep(5)
    start_attack(net)
    time.sleep(120)
  #  CLI( net )

    stop_attack()
    time.sleep(5)
    stop_monitor()
    CLI(net)
    net.stop()


if __name__ == '__main__':
    setLogLevel('info')
    run()

# Allows the file to be imported using 'mn --custom <filename> --topo custom
topos = {
    'custom': CustomTopo
}
