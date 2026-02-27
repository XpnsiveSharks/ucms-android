package com.example.ucms_android.network;

import com.example.ucms_android.model.StatusUpdateRequest;
import com.example.ucms_android.model.Ticket;
import com.example.ucms_android.model.TicketRequest;
import com.example.ucms_android.model.TicketResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface TicketService {

    @GET("api/tickets")
    Call<List<Ticket>> getTickets();

    @GET("api/tickets")
    Call<List<Ticket>> getTicketsByStatus(@Query("status") String status);

    @GET("api/tickets/{id}")
    Call<Ticket> getTicketById(@Path("id") Long id);

    @POST("api/tickets")
    Call<Ticket> createTicket(@Body TicketRequest request);

    @PATCH("api/tickets/{id}/status")
    Call<Ticket> updateTicketStatus(@Path("id") Long id, @Body StatusUpdateRequest request);

    @GET("api/tickets/{id}/responses")
    Call<List<TicketResponse>> getTicketResponses(@Path("id") Long id);
}
