package demo.async_tangocard_integration;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class OrderController {
    
    private final UserService userService;
    private final OrderService orderService;
    private final OrderPostValidator orderPostValidator;
    private final OrderViewService orderViewService;
    private final OrderProcessingService orderProcessingService;
    
    @GetMapping("/")
    public ResponseEntity<Void> get() {
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/orders")
    public OrderView post(@RequestBody OrderPost orderPost) {
        User user = userService.getCurrentUser();
        
        ValidatedOrderPost validatedOrderPost = orderPostValidator.validate(orderPost);
        
        Order order = orderService.placeOrder(user, validatedOrderPost.getAmount());
        
        orderProcessingService.queueOrder(order.getId());
        
        return orderViewService.createView(order);
    }
    
}