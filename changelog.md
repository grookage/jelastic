# Changelog

## 7.2.1-3
1. Fixed a bug in the ANY filter, which was incorrectly marking a fieldName as any instead of the constructor

## 7.2.1-2
1. Fixed the lombok annotations on the pageWindow

## 7.2.1
1. Updated to com.grookage from io.grookage

## 7.2.0-7-SNAPSHOT
1. Updated to elasticsearch rest client from the existing transport client.

## 7.2.0-6
1. Minor fixes in the loadAll method on guarding against batch fetches and avoiding multiple calls to ES. 

## 7.2.0-5
1. Added loadAll - Scroll API support to elastic search
2. Deprecated the original Query and SearchRequest, since Sorters have changed now to incorporate, field, geodistance, script and source
3. In the next major version release these shall be deprecated.

##  7.2.0-3-SNAPSHOT
1. Updated JsonTypeInfo property of Filter to 'filterType' instead of 'operator' which doesn't exists
2. Pass additional field XContentType.JSON when updateField is being called -  https://github.com/koushikr/jelastic/pull/2
3. Added JelasticException which extends from Runtime exception and all exception extends JelasticException
4. Added changelog.md file




