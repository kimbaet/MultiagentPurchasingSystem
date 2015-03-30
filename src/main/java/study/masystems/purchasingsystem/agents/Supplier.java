package study.masystems.purchasingsystem.agents;

import flexjson.JSONDeserializer;
import flexjson.JSONSerializer;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import study.masystems.purchasingsystem.GoodNeed;
import study.masystems.purchasingsystem.PurchaseProposal;
import study.masystems.purchasingsystem.defaultvalues.DataGenerator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Product supplier.
 */
public class Supplier extends Agent {

    private HashMap<String, PurchaseProposal> goods;

    @Override
    protected void setup() {
        goods = DataGenerator.getRandomGoodsTable(this.getAID());

        // Register the supplier service in the yellow pages
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("general-supplier");
        sd.setName("Goods Supplier");
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        }
        catch (FIPAException fe) {
            fe.printStackTrace();
        }

        // Add the behaviour serving queries from customer agents
        addBehaviour(new OfferRequestsServer());
    }

    private class OfferRequestsServer extends CyclicBehaviour {
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                // CFP Message received. Process it
                Map<String, GoodNeed> goodsRequest = new JSONDeserializer<Map<String, GoodNeed>>().deserialize(msg.getContent());
                ACLMessage reply = msg.createReply();
                HashMap<String, PurchaseProposal> requestedGoods = new HashMap<String, PurchaseProposal>();

                for (Map.Entry<String, GoodNeed> good : goodsRequest.entrySet()){
                    String goodName = good.getKey();
                    if (goods.containsKey(goodName)) {
                        requestedGoods.put(goodName, goods.get(goodName));
                    }
                }

                if (requestedGoods.size() != 0) {
                    // The requested goods are available for sale. Reply with the info
                    reply.setPerformative(ACLMessage.PROPOSE);
                    reply.setContent(new JSONSerializer().serialize(requestedGoods));
                }
                else {
                    // The requested book is NOT available for sale.
                    reply.setPerformative(ACLMessage.REFUSE);
                    reply.setContent("not-available");
                }
                myAgent.send(reply);
            }
            else {
                block();
            }
        }
    }  // End of inner class OfferRequestsServer
}
