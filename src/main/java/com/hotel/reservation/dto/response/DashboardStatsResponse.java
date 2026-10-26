package com.hotel.reservation.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class DashboardStatsResponse {
    private long totalRooms;
    private long availableRooms;
    private long occupiedRooms;
    private long totalReservations;
    private long confirmedReservations;
    private long checkedInReservations;
    private long cancelledReservations;
    private BigDecimal totalRevenue;
    private long todayCheckIns;
    private List<ReservationResponse> recentReservations;
    private Map<String, BigDecimal> monthlyRevenue;
    private Map<String, Long> dailyReservations;

    public DashboardStatsResponse() {
    }

    public long getTotalRooms() {
        return totalRooms;
    }

    public void setTotalRooms(long totalRooms) {
        this.totalRooms = totalRooms;
    }

    public long getAvailableRooms() {
        return availableRooms;
    }

    public void setAvailableRooms(long availableRooms) {
        this.availableRooms = availableRooms;
    }

    public long getOccupiedRooms() {
        return occupiedRooms;
    }

    public void setOccupiedRooms(long occupiedRooms) {
        this.occupiedRooms = occupiedRooms;
    }

    public long getTotalReservations() {
        return totalReservations;
    }

    public void setTotalReservations(long totalReservations) {
        this.totalReservations = totalReservations;
    }

    public long getConfirmedReservations() {
        return confirmedReservations;
    }

    public void setConfirmedReservations(long confirmedReservations) {
        this.confirmedReservations = confirmedReservations;
    }

    public long getCheckedInReservations() {
        return checkedInReservations;
    }

    public void setCheckedInReservations(long checkedInReservations) {
        this.checkedInReservations = checkedInReservations;
    }

    public long getCancelledReservations() {
        return cancelledReservations;
    }

    public void setCancelledReservations(long cancelledReservations) {
        this.cancelledReservations = cancelledReservations;
    }

    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(BigDecimal totalRevenue) {
        this.totalRevenue = totalRevenue;
    }

    public long getTodayCheckIns() {
        return todayCheckIns;
    }

    public void setTodayCheckIns(long todayCheckIns) {
        this.todayCheckIns = todayCheckIns;
    }

    public List<ReservationResponse> getRecentReservations() {
        return recentReservations;
    }

    public void setRecentReservations(List<ReservationResponse> recentReservations) {
        this.recentReservations = recentReservations;
    }

    public Map<String, BigDecimal> getMonthlyRevenue() {
        return monthlyRevenue;
    }

    public void setMonthlyRevenue(Map<String, BigDecimal> monthlyRevenue) {
        this.monthlyRevenue = monthlyRevenue;
    }

    public Map<String, Long> getDailyReservations() {
        return dailyReservations;
    }

    public void setDailyReservations(Map<String, Long> dailyReservations) {
        this.dailyReservations = dailyReservations;
    }
}
