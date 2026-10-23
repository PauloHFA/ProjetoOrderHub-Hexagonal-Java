package com.orderhub.domain.pedido;

import com.orderhub.domain.shared.Dinheiro;
import com.orderhub.domain.shared.DomainError;
import com.orderhub.domain.shared.Identificadores.Sku;
import com.orderhub.domain.shared.Quantidade;

import java.util.Objects;

/**
 * Item do Pedido (Entidade interna do agregado).
 */
public final class ItemDoPedido {
    private final Sku sku;
    private final Quantidade quantidade;
    private final Dinheiro precoUnitario;

    public ItemDoPedido(Sku sku, Quantidade quantidade, Dinheiro precoUnitario) {
        if (sku == null) {
            throw new DomainError.InvalidArgumentException("SKU do item é obrigatório");
        }
        if (quantidade == null) {
            throw new DomainError.InvalidArgumentException("Quantidade do item é obrigatória");
        }
        if (precoUnitario == null) {
            throw new DomainError.InvalidArgumentException("Preço unitário do item é obrigatório");
        }
        this.sku = sku;
        this.quantidade = quantidade;
        this.precoUnitario = precoUnitario;
    }

    public static ItemDoPedido criar(Sku sku, Quantidade quantidade, Dinheiro precoUnitario) {
        return new ItemDoPedido(sku, quantidade, precoUnitario);
    }

    public Dinheiro subtotal() {
        return precoUnitario.multiplicar(quantidade.getValor());
    }

    public Sku getSku() {
        return sku;
    }

    public Quantidade getQuantidade() {
        return quantidade;
    }

    public Dinheiro getPrecoUnitario() {
        return precoUnitario;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ItemDoPedido that)) return false;
        return Objects.equals(sku, that.sku);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sku);
    }
}

