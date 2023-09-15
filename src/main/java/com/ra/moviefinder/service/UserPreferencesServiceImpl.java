package com.ra.moviefinder.service;

import com.proto.common.Movie;
import com.proto.moviecontroller.UserPreferencesRequest;
import com.proto.moviecontroller.UserPreferencesResponse;
import com.proto.moviecontroller.UserPreferencesServiceGrpc;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import java.security.SecureRandom;

/**
 *  bidirectional streaming scenario that receives a stream of movies and responds with a shortlisted stream  of movies.
 *  The shortlisting is performed by matching the incoming  movies with the user preferences.
 *  The method signature has StreamObserver as argument as well as return type.
 */
public class UserPreferencesServiceImpl extends UserPreferencesServiceGrpc.UserPreferencesServiceImplBase {
	/**
	 * StreamObserver<UserPreferencesRequest> – used to receive stream  of messages from the client using onNext(), onError() and  onCompleted() calls
	 * 	 StreamObserver<UserPreferencesResponse> – used to respond with  stream of messages back to the client using onNext(), onError() and  onCompleted() calls
	 */
	@Override
	public StreamObserver<UserPreferencesRequest> getShortlistedMovies(StreamObserver<UserPreferencesResponse> responseObserver) {
		StreamObserver<UserPreferencesRequest> streamObserver = new StreamObserver<UserPreferencesRequest>() {
			@Override
			public void onNext(UserPreferencesRequest value) {
				if(isEligible(value.getMovie())) {
					responseObserver.onNext(UserPreferencesResponse.newBuilder().setMovie(value.getMovie()).build());
				}
			}

			@Override
			public void onError(Throwable t) {
				responseObserver.onError(Status.INTERNAL.withDescription("Internal server error").asRuntimeException());
			}

			@Override
			public void onCompleted() {
				responseObserver.onCompleted();
			}
		};
		return streamObserver;
	}

	private boolean isEligible(Movie movie) {
		return (new SecureRandom().nextInt() % 4 != 0);
	}
}
