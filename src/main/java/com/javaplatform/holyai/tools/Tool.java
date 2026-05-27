package com.javaplatform.holyai.tools;

public interface Tool {
    String getName();
    String getDescription();
    String execute(String args) throws Exception;
}
