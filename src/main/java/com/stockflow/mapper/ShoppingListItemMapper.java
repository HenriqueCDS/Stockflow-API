package com.stockflow.mapper;

import com.stockflow.domain.dto.shoppinglist.ShoppingListItemResponseDTO;
import com.stockflow.domain.entity.ShoppingListItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ShoppingListItemMapper {

    @Mapping(target = "productId", source = "product.id")
    ShoppingListItemResponseDTO toResponse(ShoppingListItem item);
}
