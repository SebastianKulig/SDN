package pl.edu.agh.kt;

import java.util.ArrayList;
import java.util.List;

import org.projectfloodlight.openflow.protocol.OFFlowDelete;
import org.projectfloodlight.openflow.protocol.OFFlowMod;
import org.projectfloodlight.openflow.protocol.OFPacketIn;
import org.projectfloodlight.openflow.protocol.OFPacketOut;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActionOutput;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.IpProtocol;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.OFGroup;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.OFVlanVidMatch;
import org.projectfloodlight.openflow.types.TransportPort;
import org.projectfloodlight.openflow.types.VlanVid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.packet.Data;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.packet.TCP;
import net.floodlightcontroller.packet.UDP;

public class BlockingRuleBuilder {

	private static final Logger logger = LoggerFactory.getLogger(BlockingRuleBuilder.class);

	public static short FLOWMOD_DEFAULT_IDLE_TIMEOUT = 0; // infinite
	public static short FLOWMOD_DEFAULT_HARD_TIMEOUT = 0; // infinite
	public static int FLOWMOD_DEFAULT_PRIORITY = 65535;

	protected static boolean FLOWMOD_DEFAULT_MATCH_VLAN = true;
	protected static boolean FLOWMOD_DEFAULT_MATCH_MAC = true;
	protected static boolean FLOWMOD_DEFAULT_MATCH_IP_ADDR = true;
	protected static boolean FLOWMOD_DEFAULT_MATCH_TRANSPORT = true;

	public static void addBlockingRule(IOFSwitch sw, IPv4Address srcIp, IPv4Address dstIp) {
		OFFlowMod.Builder fmb = sw.getOFFactory().buildFlowAdd();
		Match.Builder mb = sw.getOFFactory().buildMatch();		
		if (srcIp != null) { 
			mb.setExact(MatchField.ETH_TYPE, EthType.IPv4).setExact(MatchField.IPV4_SRC, srcIp)
			.setExact(MatchField.ETH_TYPE, EthType.IPv4).setExact(MatchField.IPV4_DST , dstIp);
			logger.info("====================== BLOCKING RULE ADDED. BLOCK SRC IP: {} TO DST IP {} ======================", srcIp, dstIp);
		}
		Match m = mb.build();

		// actions - no actions to drop packet
		OFActionOutput.Builder aob = sw.getOFFactory().actions().buildOutput();
		List<OFAction> actions = new ArrayList<OFAction>();
		actions.add(aob.build());
			
		fmb.setMatch(m).setIdleTimeout(FLOWMOD_DEFAULT_IDLE_TIMEOUT)
				.setHardTimeout(FLOWMOD_DEFAULT_HARD_TIMEOUT)
				.setPriority(FLOWMOD_DEFAULT_PRIORITY);


		// write flow to switch
		try {
			sw.write(fmb.build());
			logger.info("Flow from ip address {} dropped; match: {}", new Object[] {srcIp, m.toString() });
		} catch (Exception e) {
			logger.error("Error {}", e);
		}
	}
}
