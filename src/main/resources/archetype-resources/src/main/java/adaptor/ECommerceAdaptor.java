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
     public ECommerceAdaptor(Map<String, String> configuration, Subscriber subscriber) {
        super(configuration, subscriber);
    }
    
    final String ADAPTER_KEY = "${package}";
    public Map<String,String> getCredentials(User user){
        String s =  user == null ? null : user.getCredentialJson();
        if (s == null){
            s = "{}";
        }
        Map<String,String> headers = new HashMap<>();
        
        JSONObject o  = JSONAwareWrapper.parse(s);
        for (Object k : o.keySet()){
            headers.put((String)k,(String)o.get(k));
        }
        return headers;
    }
    
    public User getUser(Request request){
        String action = request.getContext().getAction();
        if (ObjectUtil.equals(action,"search" )){
            Intent intent = request.getMessage().getIntent();
            FulfillmentStop fulfillmentStop  = intent.getFulfillment()._getStart();
            if (fulfillmentStop != null){
                String token = fulfillmentStop.getAuthorization().getToken();
                String type = fulfillmentStop.getAuthorization().getType();
                if (ObjectUtil.equals(type,"customer")){
                    return User.findProvider(token);
                }
            }
        }
        JSONObject headers = request.getExtendedAttributes().get("headers");
        if (headers.containsKey("USER.ID")) {
            long userId = Long.parseLong((String) headers.get("USER.ID"));
            return Database.getTable(User.class).get(userId);
        }
        return null;
    }
    
    private void setErrorResponse(Request response, Exception ex){
        response.setError(new Error(){{
            if (ex instanceof BecknException) {
                setCode(((BecknException)ex).getErrorCode());
            }else{
                setCode(new SellerException.GenericBusinessError().getErrorCode());
            }
            setMessage(ex.getMessage());
        }});
    }
    
    
    @Override
    public void search(Request request, Request response) {
        Domain domain = NetworkAdaptorFactory.getInstance().getAdaptor(request.getContext().getNetworkId()).getDomains().get(request.getContext().getDomain());
        
        DomainCategory allowedDomainCategory = getProviderConfig().getDomainCategory();
        if (domain.getDomainCategory() != allowedDomainCategory){
            setErrorResponse(response,new NoDataAvailable());
            return;
        }
        //Maps to getQuote.
        User user = getUser(request);
        if (user == null || ObjectUtil.isVoid(user.getCredentialJson())){
            response.setMessage(new Message(){{
                setCatalog(new Catalog());
            }});
        }
        //
        
        Call<JSONObject> call  = new Call<JSONObject>().url(getConfiguration().get("%s.url".formatted(ADAPTER_KEY)),"/application_search_uri").
                header("content-type","application/json").method(HttpMethod.POST).
                headers(getCredentials(user)).input(new JSONObject(){{
                    put("X","x");
                    //Prepare Api input for Application's internal search/quote api.
                }}).
                inputFormat(InputFormat.JSON);
        
        Processor processor = (appResponse, becknResponse) -> {
            //Transforn appResponse to becknResponse as on_search json
        };
        processor.process(call,response);
    }
    
    @Override
    public void confirm(Request request, Request response) {
        User user = getUser(request);
        if (user == null || ObjectUtil.isVoid(user.getCredentialJson())){
            throw new RuntimeException("User Not Authorized");
        }
        
        Context context = request.getContext();
        Order order = request.getMessage().getOrder();
        FulfillmentStop start  = order.getFulfillment()._getStart();
        FulfillmentStop end = order.getFulfillment()._getEnd();
        
        //Prepare and call api to create  order.
        Call<JSONObject> call  = new Call<JSONObject>().url((getConfiguration().get("%s.url".formatted(ADAPTER_KEY))),"/application_order_confirm_api").
                header("content-type","application/json").method(HttpMethod.POST).
                headers(getCredentials(user)).input(new JSONObject(){{
                    put("X","x");
                }}).
                inputFormat(InputFormat.JSON);
        
        Processor processor = (appResponse, becknResponse) -> {
            //Transforn appResponse to becknResponse as on_search json
        };
        processor.process(call,response);
    }
    
    @Override
    public void track(Request request, Request response) {
        Order order = request.getMessage().getOrder();
        User user = getUser(request);
        
        Order lastKnown = LocalOrderSynchronizerFactory.getInstance().getLocalOrderSynchronizer(getSubscriber()).getLastKnownOrder(request.getContext().getTransactionId());
        
        
        Call<JSONObject> call  = new Call<JSONObject>().url((getConfiguration().get("%s.url".formatted(ADAPTER_KEY))),"/application_order_status/%s".formatted(order.getId())).
                header("content-type","application/json").
                headers(getCredentials(user)).
                method(HttpMethod.GET);
        
        Processor processor = (appResponse, becknResponse) -> {
            //Transforn appResponse to becknResponse as on_search json
        };
        processor.process(call,response);
        
    }
    
    @Override
    public void cancel(Request request, Request response) {
        throw new UnsupportedOperationException();
    }
    
    
    
    // Mey not need to modify below this line..
    
    public interface Processor {
        default  void process(Call<JSONObject> call , Request response){
            if (!call.hasErrors()){
                process((JSONAware) call.getResponseAsJson(), response );
            }else{
                Error error  = getError(call.getError());
                response.setError(error);
            }
        }
        void process(JSONAware appResponse, Request becknResponse);
        
        default Error getError(String error){
            return new Error(){{
                this.setMessage(error);
                this.setType(Type.DOMAIN_ERROR);
                this.setCode("Unknown");
            }};
        }
        
        
    }
    
    
    @Override
    public void select(Request request, Request response) {
        throw new UnsupportedOperationException();
    }
    
    
    @Override
    public void init(Request request, Request response) {
        response.update(request);
        response.getExtendedAttributes().setInner(request.getExtendedAttributes().getInner());
    }
    
    
    @Override
    public void update(Request request, Request response) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public void status(Request request, Request response) {
        track(request,response);
    }

}
