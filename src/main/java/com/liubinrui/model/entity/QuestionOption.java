package com.liubinrui.model.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class QuestionOption implements Serializable {
    private String result;
    private int score;
    private String value;
    private String key;
    private static final long serialVersionUID = 1L;
}
