package com.example.app.common.enums;

import lombok.Getter;

@Getter
public enum ProjectTaskEnums {

    PENDING(1, "PENDING"),
    IN_PROGRESS(2, "IN_PROGRESS"),
    COMPLETED(3, "COMPLETED");

    private final Integer statusId;
    private final String statusName;

    ProjectTaskEnums(Integer statusId, String statusName) {

        this.statusId = statusId;
        this.statusName = statusName;
    }
}
