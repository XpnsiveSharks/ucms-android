package com.example.ucms_android.network;

import com.example.ucms_android.model.ApiResponse;
import com.example.ucms_android.model.AttachmentResponse;
import com.example.ucms_android.model.CreateResponseRequest;
import com.example.ucms_android.model.StatusUpdateRequest;
import com.example.ucms_android.model.Ticket;
import com.example.ucms_android.model.TicketRequest;
import com.example.ucms_android.model.TicketResponse;

import java.util.List;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface TicketService {

    @GET("api/tickets")
    Call<ApiResponse<List<Ticket>>> getTickets(@Query("status") String status);

    @GET("api/tickets/{id}")
    Call<ApiResponse<Ticket>> getTicketById(@Path("id") Long id);

    @POST("api/tickets")
    Call<ApiResponse<Ticket>> createTicket(@Body TicketRequest request);

    @PATCH("api/tickets/{id}/status")
    Call<ApiResponse<Ticket>> updateTicketStatus(@Path("id") Long id, @Body StatusUpdateRequest request);

    @PATCH("api/tickets/{id}/confirm-resolved")
    Call<ApiResponse<Ticket>> confirmResolved(@Path("id") Long id);

    @GET("api/tickets/{id}/responses")
    Call<ApiResponse<List<TicketResponse>>> getTicketResponses(@Path("id") Long id);

    @POST("api/tickets/{id}/responses")
    Call<ApiResponse<TicketResponse>> postResponse(@Path("id") Long id, @Body CreateResponseRequest request);

    @GET("api/tickets/{id}/attachments")
    Call<ApiResponse<List<AttachmentResponse>>> getAttachments(@Path("id") Long id);

    @Multipart
    @POST("api/tickets/{id}/attachments")
    Call<ApiResponse<AttachmentResponse>> uploadAttachment(
            @Path("id") Long id,
            @Part MultipartBody.Part file
    );
}
