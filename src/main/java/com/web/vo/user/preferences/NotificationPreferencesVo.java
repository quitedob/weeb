package com.web.vo.user.preferences;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPreferencesVo {
    @NotNull
    @JsonDeserialize(using = StrictBooleanDeserializer.class)
    private Boolean newMessages;

    @NotNull
    @JsonDeserialize(using = StrictBooleanDeserializer.class)
    private Boolean follows;

    @NotNull
    @JsonDeserialize(using = StrictBooleanDeserializer.class)
    private Boolean likes;

    @NotNull
    @JsonDeserialize(using = StrictBooleanDeserializer.class)
    private Boolean comments;

    @NotNull
    @JsonDeserialize(using = StrictBooleanDeserializer.class)
    private Boolean groupInvites;

    @JsonAnySetter
    public void rejectUnknownField(String name, Object value) {
        throw new IllegalArgumentException("Unknown notification preference field: " + name);
    }
}
