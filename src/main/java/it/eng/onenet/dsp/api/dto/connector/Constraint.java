package it.eng.onenet.dsp.api.dto.connector;

import lombok.Data;

@Data
public class Constraint {
    private String leftOperand;
    private String operator;
    private String rightOperand;
}