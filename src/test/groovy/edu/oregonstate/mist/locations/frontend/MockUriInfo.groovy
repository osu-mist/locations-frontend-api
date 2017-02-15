package edu.oregonstate.mist.locations.frontend

import javax.ws.rs.core.UriInfo
import javax.ws.rs.core.UriBuilder
import javax.ws.rs.core.PathSegment
import javax.ws.rs.core.MultivaluedMap
import javax.ws.rs.core.MultivaluedHashMap

class MockUriInfo implements UriInfo {
    private MultivaluedMap<String, String> queryParameters = new MultivaluedHashMap()

    @Override
    URI getAbsolutePath() {
        throw new UnsupportedOperationException('not implemented')
    }

    @Override
    UriBuilder getAbsolutePathBuilder() {
        throw new UnsupportedOperationException('not implemented') }

    @Override
    URI getBaseUri() {
        throw new UnsupportedOperationException('not implemented')
    }

    @Override
    UriBuilder getBaseUriBuilder() {
        throw new UnsupportedOperationException('not implemented')
    }

    @Override
    List <Object> getMatchedResources() {
        throw new UnsupportedOperationException('not implemented')
    }

    @Override
    List <String> getMatchedURIs(boolean decode) {
        throw new UnsupportedOperationException('not implemented')
    }

    @Override
    String getPath(boolean decode) {
        throw new UnsupportedOperationException('not implemented')
    }

    @Override
    MultivaluedMap<String,String> getPathParameters(boolean decode) {
        throw new UnsupportedOperationException('not implemented')
    }

    @Override
    List<PathSegment> getPathSegments(boolean decode) {
        throw new UnsupportedOperationException('not implemented')
    }

    @Override
    MultivaluedMap<String, String> getQueryParameters(boolean decode) {
        this.queryParameters
    }

    @Override
    URI getRequestUri() {
        throw new UnsupportedOperationException('not implemented')
    }

    @Override
    UriBuilder getRequestUriBuilder() {
        throw new UnsupportedOperationException('not implemented')
    }

    @Override
    URI relativize (URI uri) {
        throw new UnsupportedOperationException('not implemented')
    }

    @Override
    URI resolve(URI uri) {
        throw new UnsupportedOperationException('not implemented')
    }

    @Override
    List<String> getMatchedURIs() {
        getMatchedURIs(true)
    }

    @Override
    String getPath() {
        getPath(true)
    }

    @Override
    MultivaluedMap<String, String> getPathParameters() {
        getPathParameters(true)
    }

    @Override
    List<PathSegment> getPathSegments() { getPathSegments(true) }

    @Override
    MultivaluedMap<String, String> getQueryParameters() {
        getQueryParameters(true)
    }
}