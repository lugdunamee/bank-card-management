package org.example.bankcardmanagement.card.api.mapper;

import org.example.bankcardmanagement.card.api.dto.CardCreateDto;
import org.example.bankcardmanagement.card.api.dto.CardDto;
import org.example.bankcardmanagement.card.api.dto.CardRequestDto;
import org.example.bankcardmanagement.card.domain.Card;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface CardMapper {

    @Mapping(target = "maskedNumber", source = "numberLast4", qualifiedByName = "maskLast4")
    @Mapping(target = "owner", source = "ownerUser.username")
    CardDto toDto(Card card);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "numberEncrypted", ignore = true)
    @Mapping(target = "numberLast4", ignore = true)
    @Mapping(target = "ownerUser", ignore = true)
    Card toEntity(CardCreateDto dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "numberEncrypted", ignore = true)
    @Mapping(target = "numberLast4", ignore = true)
    @Mapping(target = "ownerUser", ignore = true)
    Card toEntity(CardRequestDto dto);

    @Named("maskLast4")
    default String maskLast4(String last4) {
        if (last4 == null || last4.isBlank()) {
            return "";
        }

        String digits = last4.replaceAll("\\s+", "");
        String normalizedLast4 = digits.length() <= 4 ? digits : digits.substring(digits.length() - 4);
        return "**** **** **** " + normalizedLast4;
    }
}
