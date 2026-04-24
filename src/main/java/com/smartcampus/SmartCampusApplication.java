package com.smartcampus;

import javax.ws.rs.core.Application;

/**
 * JAX-RS Application configuration class.
 *
 * The base URL for all REST endpoints is configured in web.xml via servlet mapping.
 * When deployed to Tomcat as a WAR, the full base URL is:
 *   http://localhost:8080/smart-campus-api/api/v1/
 *
 * Lifecycle note: By default, JAX-RS creates a new instance of each resource class
 * per HTTP request (request-scoped). Since we use static ConcurrentHashMap data stores
 * in a dedicated DataStore class, state is shared safely across all request instances
 * without the risk of data loss between requests.
 */
public class SmartCampusApplication extends Application {
    // Jersey auto-discovers resource classes via package scanning configured in web.xml
}
