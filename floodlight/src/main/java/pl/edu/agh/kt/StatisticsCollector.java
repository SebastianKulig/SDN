package pl.edu.agh.kt;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IOFSwitch;

import org.projectfloodlight.openflow.protocol.OFFlowStatsEntry;
import org.projectfloodlight.openflow.protocol.OFFlowStatsReply;
import org.projectfloodlight.openflow.protocol.OFPortStatsEntry;
import org.projectfloodlight.openflow.protocol.OFPortStatsReply;
import org.projectfloodlight.openflow.protocol.OFStatsReply;
import org.projectfloodlight.openflow.protocol.OFStatsRequest;
import org.projectfloodlight.openflow.protocol.OFStatsType;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.TableId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ListenableFuture;

public class StatisticsCollector {
	private static final Logger logger = LoggerFactory
			.getLogger(StatisticsCollector.class);
	private IOFSwitch sw;
	private FloodlightContext cntx;
	
	public static final int PORT_STATISTICS_POLLING_INTERVAL = 3000; // in ms
	private static StatisticsCollector singleton;
	public static Map<Integer, Long> previousValuesPorts = new HashMap();
	public static Map<IPv4Address, Long> previousValuesFlows = new HashMap();

	public class StatisticsPoller extends TimerTask {
		private final Logger logger = LoggerFactory.getLogger(StatisticsPoller.class);
		private OFStatsType statsType;
		private Match match;
		
		public StatisticsPoller(OFStatsType inStatsType) {
			statsType = inStatsType;
		}

		@SuppressWarnings("unchecked")
		@Override
		public void run() {
			synchronized (StatisticsCollector.this) {
				if (sw == null) { // no switch
					logger.error("run() end (no switch)");
					return;
				}
				ListenableFuture<?> future;
				List<OFStatsReply> values = null;
				OFStatsRequest<?> req = null;
				switch (this.statsType) {
				
				case FLOW:
					match = sw.getOFFactory().buildMatch().build();
		            req = sw.getOFFactory().buildFlowStatsRequest()
		                    .setMatch(match)
		                    .setOutPort(OFPort.ANY)
		                    .setTableId(TableId.ALL)
		                    .build();
					break;
				case PORT:
					req = sw.getOFFactory().buildPortStatsRequest()
							.setPortNo(OFPort.ANY).build();
					break;
				default:
					logger.error("Stats Request Type {} not implemented yet", this.statsType.name());
					break;
				}
				
				try {
					if (req != null) {
						future = sw.writeStatsRequest(req);
						values = (List<OFStatsReply>) future.get(
								PORT_STATISTICS_POLLING_INTERVAL *1000 / 2,
								TimeUnit.MILLISECONDS); 
					}
					switch (this.statsType) {
					case FLOW:
						OFFlowStatsReply fsr = (OFFlowStatsReply) values.get(0);
						for (OFFlowStatsEntry pse : fsr.getEntries()) {
							IPv4Address srcIp = pse.getMatch().get(MatchField.IPV4_SRC);
							
							if (previousValuesFlows.containsKey(srcIp)) {							
                                double tput = 8.0 * (pse.getByteCount().getValue() - previousValuesFlows.get(srcIp)) / PORT_STATISTICS_POLLING_INTERVAL * 1000.0 / 1024 / 1024;                                                         
                                logger.info("\tSRC IP: {}, speed: {} MB/s", srcIp, tput);
                                logger.info("\tSRC IP: {}, packets count: {}", srcIp, pse.getPacketCount().getValue());
                                if(pse.getPacketCount().getValue() > 300L){
                                	logger.info("\t====================== ATTACK DETECTED - ADDING BLOCKING RULE ======================");                                	                                            	
                                	BlockingRuleBuilder.addBlockingRule(sw, pse.getMatch().get(MatchField.IPV4_SRC));
                                }                               
							}
							previousValuesFlows.put(pse.getMatch().get(MatchField.IPV4_SRC), pse.getByteCount().getValue());
							
						}
						break;
					case PORT:
						OFPortStatsReply psr = (OFPortStatsReply) values.get(0);
						logger.info("Switch id: {}", sw.getId());
						for (OFPortStatsEntry pse : psr.getEntries()) {
							if (pse.getPortNo().getPortNumber() > 0) {
	                            if (previousValuesPorts.containsKey(pse.getPortNo().getPortNumber())) {
	                                double tput = 8.0 * (pse.getTxBytes().getValue() - previousValuesPorts.get(pse.getPortNo().getPortNumber())) / PORT_STATISTICS_POLLING_INTERVAL * 1000.0 / 1024 / 1024;
	                                logger.info("\tport number: {}, speed: {} MB/s", pse.getPortNo().getPortNumber(), tput);
	                                previousValuesPorts.put(pse.getPortNo().getPortNumber(), pse.getTxBytes().getValue());	                                
	                            } else {
	                                previousValuesPorts.put(pse.getPortNo().getPortNumber(), pse.getTxBytes().getValue());
	                            }
							}
						}
						break;
					default:
						logger.error("Stats Request Type {} not implemented yet", this.statsType.name());
						break;
					}

				} catch (InterruptedException | ExecutionException | TimeoutException ex) {
					logger.error("Error during statistics polling ", ex);
				}
			}
		}
	}

	private StatisticsCollector(IOFSwitch sw, FloodlightContext cntx) {
		this.sw = sw;
		this.cntx = cntx;
		new Timer().scheduleAtFixedRate(new StatisticsPoller(OFStatsType.FLOW), 0, PORT_STATISTICS_POLLING_INTERVAL);
		//new Timer().scheduleAtFixedRate(new StatisticsPoller(OFStatsType.PORT), 0, PORT_STATISTICS_POLLING_INTERVAL);
	}

	public static StatisticsCollector getInstance(IOFSwitch sw, FloodlightContext cntx) {
		synchronized (StatisticsCollector.class) {
			if (singleton == null) {
				singleton = new StatisticsCollector(sw, cntx);
			}
		}
		return singleton;
	}
}