package com.ra.moviefinder.service;

import com.proto.common.Movie;
import com.proto.moviecontroller.RecommenderRequest;
import com.proto.moviecontroller.RecommenderResponse;
import com.proto.moviecontroller.RecommenderServiceGrpc;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

/**
 * client streaming
 * The stream of movies that are shortlisted from User Preferences are accepted and a movie is picked as recommended and published back to the client.
 * The method signature is similar to the above case, however, the onNext() of responseObserver is invoked only once to send a solitary  message to the client.
 */
public class RecommenderServiceImpl extends RecommenderServiceGrpc.RecommenderServiceImplBase {
	@Override
	public StreamObserver<RecommenderRequest> getRecommendedMovie(StreamObserver<RecommenderResponse> responseObserver) {
		StreamObserver<RecommenderRequest> streamObserver = new StreamObserver<RecommenderRequest>() {
			List<Movie> movies = new ArrayList<>();

			public void onNext(RecommenderRequest value) {
				movies.add(value.getMovie());
			}

			public void onError(Throwable t) {
				responseObserver.onError(Status.INTERNAL.withDescription("Internal server error").asRuntimeException());
			}

			public void onCompleted() {
				if(movies.size() > 0) {
					responseObserver.onNext(RecommenderResponse.newBuilder().setMovie(findMovieForRecommendation(movies)).build());
					responseObserver.onCompleted();
				} else {
					responseObserver.onError(Status.NOT_FOUND.withDescription("Sorry, found no movies to recommend!").asRuntimeException());
				}
			}
		};
		return streamObserver;
	}

	private Movie findMovieForRecommendation(List<Movie> movies) {
		int random = new SecureRandom().nextInt(movies.size());
		return movies.stream().skip(random).findAny().get();
	}
}
