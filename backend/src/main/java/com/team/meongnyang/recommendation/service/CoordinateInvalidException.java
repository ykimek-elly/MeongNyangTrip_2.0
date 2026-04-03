package com.team.meongnyang.recommendation.service;

public class CoordinateInvalidException extends RuntimeException {

    public CoordinateInvalidException(double latitude, double longitude) {
        super("user coordinates are not initialized: latitude=" + latitude + ", longitude=" + longitude);
    }
}
