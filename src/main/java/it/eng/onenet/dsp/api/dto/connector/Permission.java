package it.eng.onenet.dsp.api.dto.connector;

import java.util.List;

import lombok.Data;

@Data
public class Permission {
    private String assigner;
    private String assignee;
    private String target;
    private String action;
    private List<Constraint> constraint;
}