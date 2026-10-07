package com.stockflow.mapper;

import com.stockflow.domain.dto.membership.MemberResponseDTO;
import com.stockflow.domain.dto.user.UserProfileResponseDTO;
import com.stockflow.domain.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserProfileResponseDTO toProfileResponse(User user);

    MemberResponseDTO toMemberResponse(User user);
}
