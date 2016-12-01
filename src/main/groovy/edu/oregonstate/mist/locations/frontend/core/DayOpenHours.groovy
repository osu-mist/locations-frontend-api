package edu.oregonstate.mist.locations.frontend.core

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnore

class DayOpenHours {
    @JsonFormat(shape=JsonFormat.Shape.STRING, pattern="yyyy-MM-dd'T'HH:mm:ss'Z'", timezone="UTC")
    Date start

    @JsonFormat(shape=JsonFormat.Shape.STRING, pattern="yyyy-MM-dd'T'HH:mm:ss'Z'", timezone="UTC")
    Date end

    /**
     * Property used to identify events in a series.
     *
     * An event can be overwritten by a new event by using the same UID with a
     * different sequence
     *
     */
    @JsonIgnore
    String uid

    @JsonIgnore
    Integer sequence
}
