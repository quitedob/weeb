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
public class PrivacyPreferencesVo {
    @NotNull
    @JsonDeserialize(using = StrictBooleanDeserializer.class)
    private Boolean onlineVisible;

    @NotNull
    @JsonDeserialize(using = StrictBooleanDeserializer.class)
    private Boolean allowMessages;

    @NotNull
    @JsonDeserialize(using = StrictBooleanDeserializer.class)
    private Boolean showFollows;

    @JsonAnySetter
    public void rejectUnknownField(String name, Object value) {
        throw new IllegalArgumentException("Unknown privacy preference field: " + name);
    }
}
