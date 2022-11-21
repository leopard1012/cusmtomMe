package com.custom.me.order.service;

import com.custom.me.order.entity.Product;
import com.custom.me.order.entity.ProductRepository;
import com.custom.me.order.service.dto.MaterialResponse;
import com.custom.me.order.service.dto.OrderRequest;
import com.custom.me.order.service.dto.OrderResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderService {
    private LocalDateTime processDt = LocalDateTime.of(2022,8,1,9,0,0);
    private Map<String, Integer> machine = new HashMap<>(); // Map.of("A",200, "B",200, "C", 200, "D", 200);
    private int todayProductCount = 0;
    private boolean available = true;
    private LocalDateTime refillStartDt = null;

    private boolean isClose = false;

    private final ProductRepository productRepository;

    /*
        제품생산에 걸리는 시간 : 1초(1분)
     */

    public OrderResponse order(OrderRequest request) {
        OrderResponse response = new OrderResponse();
        String orderDate = "20" + request.getOrder_date();

        String processToday = processDt.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        if (!processToday.equals(orderDate)) { // 주문일이 잘못됨 (시스템상 당일이 아님)
            response.setOrder_number(request.getOrder_number());
            response.setOrder(request.getOrder());
            response.setError("주문일이 잘못되었습니다.");
            return response;
        }

        // orderCode 형식도 체크해야함 A10, B2C8 형태여야함.
        if (!checkOrderCode(request.getOrder())) {
            response.setOrder_number(request.getOrder_number());
            response.setOrder(request.getOrder());
            response.setError("주문코드가 잘못되었습니다.");
            return response;
        }

        Product preProduct = productRepository.findByOrderNumber(request.getOrder_number()).orElse(null);

        if (preProduct != null) { // 주문번호는 고유값 중복되면 에러
            response.setOrder_number(request.getOrder_number());
            response.setOrder(request.getOrder());
            response.setError("이미등록된 주문번호입니다.");
            return response;
        } else {
            Product product = new Product();
            product.setOrderNumber(request.getOrder_number());
            product.setOrderCode(request.getOrder());
            product.setOrderDate(orderDate);
            product.setOrderStatus("주문접수");

            long waitCount = productRepository.countByOrderStatus("주문접수") + todayProductCount;
            long delay = isClose ? (waitCount/30) + 1 : waitCount/30;
            String sendDate = processDt.plusDays(delay).format(DateTimeFormatter.ofPattern("yyyyMMdd"));

            response.setOrder_number(product.getOrderNumber());
            response.setOrder(product.getOrderCode());
            response.setStatus("주문접수");
            response.setSend_date(sendDate);
            if (delay >= 7) {
                product.setOrderStatus("생산지연");
                response.setStatus("생산지연");
                response.setError("생산지연으로 인한 주문취소");
            }
            product.setSendDate(sendDate);
            productRepository.save(product);
            log.info("주문접수 number={}, code={}", product.getOrderNumber(), product.getOrderCode());
        }

        return response;
    }

    public OrderResponse findByOrderNumber(String orderNumber) {
        OrderResponse response = new OrderResponse();
        Product product = productRepository.findByOrderNumber(orderNumber).orElse(null);
        if (product == null) {
            response.setOrder_number(orderNumber);
            response.setOrder(response.getOrder());
            response.setError("해당 주문번호의 주문내역이 없습니다.");
        } else {
            response.setOrder_number(product.getOrderNumber());
            response.setOrder(product.getOrderCode());
            response.setSend_date(product.getSendDate());
            response.setStatus(product.getOrderStatus());
        }

        return response;
    }

    public MaterialResponse getMachine() {
        MaterialResponse response = new MaterialResponse();
        response.setMaterial(machine);
        response.setResult("원료재고조회");
        return response;
    }

    public MaterialResponse updateMaterial(String materialCode, String type) {
        MaterialResponse response = new MaterialResponse();
        if (materialCode.length() == 1 &&
                (  (materialCode.charAt(0) >= 65 && materialCode.charAt(0) <= 90)
                || (materialCode.charAt(0) >= 97 && materialCode.charAt(0) <= 122) )
        ) {
            if (type.equals("DELETE")) {
                if (machine.remove(materialCode) == null) {
                    response.setResult("존재하지 않은 효능입니다.");
                } else {
                    response.setResult("원료삭제");
                }
            } else {
                if (machine.containsKey(materialCode) && machine.get(materialCode) == 200) { // 이미 가득차 있는 경우
                    response.setResult("원료가 이미가득차 있습니다.");
                } else {
                    if (!machine.containsKey(materialCode) && machine.size() >= 10) {
                        response.setResult("연료종류는 최대 10가지입니다.");
                    } else {
                        machine.put(materialCode, 200);
                        refillStartDt = processDt;
                        response.setResult("원료추가");
                    }
                }
            }
            response.setMaterial(machine);
        } else {
            response.setResult("잘못된 원료코드");
        }

        return response;
    }

    private boolean checkOrderCode(String orderCode) {
        if (orderCode.length() == 3) {
            if ((orderCode.charAt(0) >= 65 && orderCode.charAt(0) <= 90)
                    || (orderCode.charAt(0) >= 97 && orderCode.charAt(0) <= 122)) {
                try {
                    int need = Integer.parseInt(orderCode.substring(1, 3));
                    if (need == 10) return true;
                    else return false;
                } catch (Exception e) {
                    return false;
                }
            } else { // 알파벳이 아닐경우
                return false;
            }
        } else if (orderCode.length() == 4) {
            String[] arCode = orderCode.split("");
            int need1, need2;
            if ((arCode[0].charAt(0) >= 65 && arCode[0].charAt(0) <= 90)
                    || (arCode[0].charAt(0) >= 97 && arCode[0].charAt(0) <= 122)) {
                try {
                    need1 = Integer.parseInt(arCode[1]);
                    if (need1 >= 10 || need1 < 1) return false;
                } catch (Exception e) {
                    return false;
                }
            } else { // 알파벳이 아닐경우
                return false;
            }
            if ((arCode[2].charAt(0) >= 65 && arCode[2].charAt(0) <= 90)
                    || (arCode[2].charAt(0) >= 97 && arCode[2].charAt(0) <= 122)) {
                try {
                    need2 = Integer.parseInt(arCode[3]);
                    if (need2 >= 10 || need2 < 1) return false;
                } catch (Exception e) {
                    return false;
                }
            } else { // 알파벳이 아닐경우
                return false;
            }

            if (need1 + need2 == 10) return true;
            else return false;
        }
        return false;
    }

    // -1: 등록되지 않은 원료, 0: 원료부족, 1: 생산가능
    private int checkMaterial(String orderCode) {
        if (orderCode.length() > 3) { // 2가지 혼합
            String[] arCode = orderCode.split("");
            int need1 = Integer.parseInt(arCode[1]);
            int need2 = Integer.parseInt(arCode[3]);

            if (machine.containsKey(arCode[0]) && machine.containsKey(arCode[2])) { // 원료가 존재할 경우

                if (machine.get(arCode[0]) >= need1 && machine.get(arCode[2]) >= need2) { // 효능생산가능
                    machine.put(arCode[0], machine.get(arCode[0]) - need1);
                    machine.put(arCode[2], machine.get(arCode[2]) - need2);
                    return 1;
                } else {
                    return 0;
                }
            } else {
                return -1;
            }

        } else {
            String code = orderCode.substring(0,1);

            if (machine.containsKey(code)) { // 원료가 존재하는 경우
                if (machine.get(code) >= 10) { // 1개 원료만 있는경우 비율은 무조건 10
                    machine.replace(code, machine.get(code) - 10);
                    return 1;
                } else {
                    return 0;
                }
            } else {
                return -1;
            }
        }
    }

    @Transactional
    @Scheduled(cron = "0/1 * * * * *")
    public void convDate() {
        if (processDt.equals(LocalDateTime.of(2022,8,1,9,0,0))) {
            // 맨처음 초기화
            machine.put("A",200);
            machine.put("B",200);
            machine.put("C",200);
            machine.put("D",200);
        }

        productRepository.updateProductStatus("제품생산 중", "제품생산 완료"); // 제품생산에 1초가 소요되기 때문에 이전상태가 '제품생산 중'의 경우 1초지나서 실행될 경우 '제품생산 완료'로 일괄처리
        if (processDt.getHour() >= 9 && processDt.getHour() < 17) { // 근무시간
            isClose = false;
            productRepository.updateProductStatus("제품생산 완료", "발송준비 중"); // 근무시간에 제품생산완료된 건은 당일 발송예정
            if (processDt.getHour() == 9 && processDt.getMinute() == 0) { // 근무시작 시각
                todayProductCount = 0;
            }

            if (processDt.getMinute() == 0) { // 근무시간중 매정각에 설비상태 로그출력
                List<Product> waitProducts = productRepository.findByOrderStatusOrderByIdAsc("주문접수");
                log.info("생산대기 주문목록 : {}, 총 {}건", waitProducts, waitProducts.size());
                log.info("원료잔량 : {}", machine);
                log.info("현재시간 : {}", processDt);
                log.info("금일 처리된 물량 : {}", todayProductCount);
            }

            if (!ObjectUtils.isEmpty(refillStartDt) && refillStartDt.plusMinutes(40).isAfter(processDt)) { // 원료보충중
                available = false;
            } else {
                if (todayProductCount >= 30) {
                    available = false;
                } else {
                    available = true;
                }
            }

            if (available) {
                Product waitProduct = productRepository.findFirstByOrderStatusOrderByIdAsc("원료부족");
                // 효능단종으로 생산하지 못했던 제품이 있으면 해당 제품을 먼저 체크하여 생산
                if (waitProduct != null && checkMaterial(waitProduct.getOrderCode()) == 1) {
                    waitProduct.setOrderStatus("제품생산 중");
                    productRepository.save(waitProduct);
                    todayProductCount++;
                } else {
                    Product receiptProduct = productRepository.findFirstByOrderStatusOrderByIdAsc("주문접수");

                    if (receiptProduct != null) {
                        if (checkMaterial(receiptProduct.getOrderCode()) == 1) {
                            receiptProduct.setOrderStatus("제품생산 중");
                            todayProductCount++;
                        } else { // 원료부족인 경우
                            receiptProduct.setOrderStatus("원료부족");
                        }
                        productRepository.save(receiptProduct);
                    }
                }
            }

            // 근무시간에는 1초가 1분
            processDt = processDt.plusMinutes(1);
        } else { // 근무시간 외
            isClose = true;
            productRepository.updateProductStatus("발송준비 중", "발송완료"); // 발송준비 중 상태건은 근무시간 이후 일괄발송

            // 근무시간외 16시간은 960분으로 30초간 960분이 지나려면 1초에 32분씩 증가
            processDt = processDt.plusMinutes(32);
            available = false;
        }

//        log.info("현재시각 : {}, 처리된 물량 : {}", processDt, todayProductCount);
    }
}
