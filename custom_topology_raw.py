from mininet.topo import Topo


class CustomTopo(Topo):

    def __init__(self):
	Topo.__init__(self)
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


topos = {
    'custom': CustomTopo
}

