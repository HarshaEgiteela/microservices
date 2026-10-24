
package com.example.orderservice.service;

import com.example.orderservice.entity.OrderEntity;
import com.example.orderservice.entity.OrderStatus;
import com.example.orderservice.repository.OrderRepository;

import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PendingCancellationScheduler {

    private static final Logger log =
            LoggerFactory.getLogger(PendingCancellationScheduler.class);

    private final OrderRepository orderRepository;
    private final OrderService orderService;

    // Automatically check every 30 seconds
    @Scheduled(fixedDelay = 30000, initialDelay = 30000)
    public void retryPendingCancellations() {

        List<OrderEntity> pendingOrders =
                orderRepository.findByStatus(OrderStatus.CANCEL_PENDING);

        if (pendingOrders.isEmpty()) {
            return;
        }

        log.info(
                "Found {} pending cancellations",
                pendingOrders.size()
        );

        for (OrderEntity order : pendingOrders) {

            try {
                log.info(
                        "Retrying cancellation for order {}",
                        order.getId()
                );

                orderService.cancelOrder(order.getId());

                log.info(
                        "Cancellation completed for order {}",
                        order.getId()
                );

            } catch (Exception ex) {

                log.error(
                        "Automatic cancellation retry failed for order {}: {}",
                        order.getId(),
                        ex.getMessage(),
                        ex
                );
            }
        }
    }
}

