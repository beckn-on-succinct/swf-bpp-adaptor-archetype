package ${package}.adaptor;

import com.venky.swf.plugins.beckn.messaging.Subscriber;
import in.succinct.beckn.Items;
import in.succinct.beckn.Locations;
import in.succinct.beckn.Order;
import in.succinct.beckn.Request;
import in.succinct.bpp.core.adaptor.CommerceAdaptor;
import in.succinct.bpp.core.db.model.BecknOrderMeta;

import java.util.Map;

public class ECommerceAdaptor extends CommerceAdaptor {
     public ECommerceAdaptor(Map<String,String> configuration, Subscriber subscriber){
        super(configuration,subscriber);
         getProviderConfig().getSupportContact().setEmail(getSupportEmail());
         getProviderConfig().setLocation(getProviderLocations().get(0));
     }

    private String getSupportEmail() {
         return null; //Get from Application.
    }

    @Override
    public Locations getProviderLocations() {
        return null;
    }

    @Override
    public Items getItems() {
        return null;
    }

    @Override
    public boolean isTaxIncludedInPrice() {
        return false;
    }

    @Override
    public Order initializeDraftOrder(Request request) {
        return null;
    }

    @Override
    public Order confirmDraftOrder(Order draftOrder, BecknOrderMeta meta) {
        return null;
    }

    @Override
    public Order getStatus(Order order) {
        return null;
    }

    @Override
    public Order cancel(Order order) {
        return null;
    }

    @Override
    public String getTrackingUrl(Order order) {
        return null;
    }


}
