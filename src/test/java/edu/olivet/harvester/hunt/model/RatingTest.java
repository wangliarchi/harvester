package edu.olivet.harvester.hunt.model;

import edu.olivet.harvester.hunt.model.Rating.RatingType;
import org.testng.annotations.Test;


public class RatingTest {
    @Test
    public void score() {

        Rating rating = new Rating(99, 2074, RatingType.Last30Days);
        System.out.println(rating.score());

        rating = new Rating(100, 800, RatingType.Last30Days);
        System.out.println(rating.score());

        rating = new Rating(99, 484, RatingType.Last30Days);
        System.out.println(rating.score());
        //98%, 283
        rating = new Rating(98, 283, RatingType.Last30Days);
        System.out.println(rating.score());
        //95%, 226
        rating = new Rating(95, 226, RatingType.Last30Days);
        System.out.println(rating.score());
        //100%, 17
        rating = new Rating(100, 17, RatingType.Last30Days);
        System.out.println(rating.score());
        //100%, 2
        rating = new Rating(100, 2, RatingType.Last30Days);
        System.out.println(rating.score());

        //100%, 2
        rating = new Rating(100, 377, RatingType.Last30Days);
        System.out.println(rating.score());

        //100%, 2
        rating = new Rating(97, 3215, RatingType.Last30Days);
        System.out.println(rating.score());
    }

}