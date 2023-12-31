package com.ra.moviefinder.service;

import com.proto.moviecontroller.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * manages the gRPC integration with the other three services.
 * - Accepts the Genre input and fetches list of movies from MovieStore service
 * - Sends the stream of movie requests to UserPreferences service that sends back a shortlisted stream of movie messages as response.
 * - Sends the shortlisted movie messages to Recommender service to recommend one of the movies
 * - Responds to the MovieFinderClient request with the resulting movie returned by the Recommender service
 */
public class MovieControllerServiceImpl extends MovieControllerServiceGrpc.MovieControllerServiceImplBase {
	// Channels are established for connecting to the respective gRPC servers
	public static final int MOVIES_SERVICE_PORT = 50052;
	public static final int USER_PREFERENCES_SERVICE_PORT = 50053;
	public static final int RECOMMENDER_SERVICE_PORT = 50054;

	@Override
	public void getMovie(MovieRequest request, StreamObserver<MovieResponse> responseObserver) {
		String userId = request.getUserid();
		// Channels are then used to setup the client stubs for invoking the remote service calls.
		MovieStoreServiceGrpc.MovieStoreServiceBlockingStub movieStoreClient = MovieStoreServiceGrpc.newBlockingStub(getChannel(MOVIES_SERVICE_PORT));
		UserPreferencesServiceGrpc.UserPreferencesServiceStub userPreferencesClient = UserPreferencesServiceGrpc.newStub(getChannel(USER_PREFERENCES_SERVICE_PORT));
		RecommenderServiceGrpc.RecommenderServiceStub recommenderClient = RecommenderServiceGrpc.newStub(getChannel(RECOMMENDER_SERVICE_PORT));

		// CountDownLatch is configured to ensure that the main thread waits until the asynchronous stream observers complete their executions.
		CountDownLatch latch = new CountDownLatch(1);
		StreamObserver<RecommenderRequest> recommenderRequestObserver = recommenderClient.getRecommendedMovie(new StreamObserver<RecommenderResponse>() {
			public void onNext(RecommenderResponse value) {
				responseObserver.onNext(MovieResponse.newBuilder().setMovie(value.getMovie()).build());
				System.out.println("Recommended movie " + value.getMovie());
			}

			public void onError(Throwable t) {
				responseObserver.onError(t);
				latch.countDown();
			}

			public void onCompleted() {
				responseObserver.onCompleted();
				latch.countDown();
			}
		});
		StreamObserver<UserPreferencesRequest> streamObserver = userPreferencesClient.getShortlistedMovies(new StreamObserver<UserPreferencesResponse>() {
			public void onNext(UserPreferencesResponse value) {
				recommenderRequestObserver.onNext(RecommenderRequest.newBuilder().setUserid(userId).setMovie(value.getMovie()).build());
			}

			public void onError(Throwable t) {
			}

			@Override
			public void onCompleted() {
				recommenderRequestObserver.onCompleted();
			}
		});
		movieStoreClient.getMovies(MovieStoreRequest.newBuilder().setGenre(request.getGenre()).build()).forEachRemaining(response -> {
			streamObserver.onNext(UserPreferencesRequest.newBuilder().setUserid(userId).setMovie(response.getMovie()).build());
		});
		streamObserver.onCompleted();
		try {
			latch.await(3L, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	// configured usePlainText() to deactivate TLS checking.
	private ManagedChannel getChannel(int port) {
		return ManagedChannelBuilder.forAddress("localhost", port).usePlaintext().build();
	}
}