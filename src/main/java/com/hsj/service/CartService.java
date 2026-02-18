package com.hsj.service;

import com.hsj.dto.cart.CartAddRequest;
import com.hsj.dto.cart.CartResponse;
import com.hsj.entity.Cart;
import com.hsj.entity.CartItem;
import com.hsj.entity.Member;
import com.hsj.entity.Product;
import com.hsj.exception.ErrorCode;
import com.hsj.exception.NotFoundException;
import com.hsj.repository.CartRepository;
import com.hsj.repository.MemberRepository;
import com.hsj.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CartService {

    private final CartRepository cartRepository;
    private final MemberRepository memberRepository;
    private final ProductRepository productRepository;

    public CartResponse getCart(Long memberId) {
        Cart cart = getOrCreateCart(memberId);
        return toResponse(cart);
    }

    @Transactional
    public CartResponse addItem(Long memberId, CartAddRequest request) {
        Cart cart = getOrCreateCart(memberId);
        Product product = productRepository.findByIdAndDeletedFalse(request.getProductId())
                .orElseThrow(() -> new NotFoundException(ErrorCode.PRODUCT_NOT_FOUND));

        cart.getCartItems().stream()
                .filter(i -> i.getProduct().getId().equals(request.getProductId()))
                .findFirst()
                .ifPresentOrElse(
                        existing -> existing.changeQuantity(existing.getQuantity() + request.getQuantity()),
                        () -> {
                            CartItem item = CartItem.builder()
                                    .product(product)
                                    .quantity(request.getQuantity())
                                    .build();
                            cart.addItem(item);
                        }
                );

        return toResponse(cart);
    }

    @Transactional
    public CartResponse updateItemQuantity(Long memberId, Long productId, int quantity) {
        Cart cart = getOrCreateCart(memberId);
        cart.getCartItems().stream()
                .filter(i -> i.getProduct().getId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException(ErrorCode.PRODUCT_NOT_FOUND))
                .changeQuantity(quantity);
        return toResponse(cart);
    }

    @Transactional
    public CartResponse removeItem(Long memberId, Long productId) {
        Cart cart = getOrCreateCart(memberId);
        cart.getCartItems().removeIf(i -> i.getProduct().getId().equals(productId));
        return toResponse(cart);
    }

    @Transactional
    public void clearCart(Long memberId) {
        Cart cart = getOrCreateCart(memberId);
        cart.clear();
    }

    @Transactional
    public Cart getOrCreateCart(Long memberId) {
        return cartRepository.findByMemberIdAndDeletedFalse(memberId)
                .orElseGet(() -> {
                    Member member = memberRepository.findById(memberId)
                            .orElseThrow(() -> new NotFoundException(ErrorCode.MEMBER_NOT_FOUND));
                    return cartRepository.save(new Cart(member));
                });
    }

    private CartResponse toResponse(Cart cart) {
        var items = cart.getCartItems().stream()
                .map(i -> CartResponse.CartItemResponse.builder()
                        .cartItemId(i.getId())
                        .productId(i.getProduct().getId())
                        .productName(i.getProduct().getName())
                        .price(i.getProduct().getPrice())
                        .quantity(i.getQuantity())
                        .subtotal(i.getProduct().getPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                        .build())
                .toList();

        BigDecimal totalPrice = items.stream()
                .map(CartResponse.CartItemResponse::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CartResponse.builder()
                .cartId(cart.getId())
                .items(items)
                .totalPrice(totalPrice)
                .build();
    }
}
