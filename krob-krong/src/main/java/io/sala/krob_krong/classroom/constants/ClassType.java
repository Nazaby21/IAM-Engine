package io.sala.krob_krong.classroom.constants;

import com.fasterxml.jackson.annotation.JsonCreator;
import io.sala.krob_krong.common.exceptions.BusinessException;
import io.sala.krob_krong.iam.error.IAMErrorCode;

public enum ClassType {
    ONLINE,
    OFFLINE;

    @JsonCreator
    public static ClassType from(String value){
        if(value == null){
            return null;
        }try{
            return ClassType.valueOf(value.trim().toUpperCase());
        }catch (BusinessException e){
            throw new BusinessException(IAMErrorCode.INVALID_CLASS_TYPE);
        }

    }

}
