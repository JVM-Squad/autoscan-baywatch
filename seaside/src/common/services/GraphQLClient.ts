import {Observable} from "rxjs";
import {fromFetch} from "rxjs/fetch";
import {ConstantHttpHeaders, ConstantMediaTypes} from "@/constants";

import notificationService from '@/services/notification/NotificationService';
import {Severity} from "@/services/notification/Severity.enum";
import {NotificationCode} from "@/services/notification/NotificationCode.enum";
import {map, switchMap} from "rxjs/operators";
import {UnauthorizedError} from "@/common/errors/UnauthorizedError";
import {UnknownFetchError} from "@/common/errors/UnknownFetchError";
import {GraphqlResponse, INTERNAL_ERROR, INVALID_SYNTAX, UNAUTHORIZED} from "@/common/model/GraphqlResponse.type";
import {handleStatusCodeErrors} from "@/common/services/common";

const baseUrl = import.meta.env.VITE_API_BASE_URL + import.meta.env.VITE_GQL_ENDPOINT;

/**
 * Minify a GraphQL request body
 * @param gql   The request body
 * @private Minified request body
 */
function gqlMinify(gql: string): string {
    return gql
        .replace('#graphql', '')
        // replace multiple whitespace with a single
        .replace(/(\b|\B)\s+(\b|\B)/gm, ' ')
        // remove all whitespace between everything except for word and word boundaries
        .replace(/(\B)\s+(\B)|(\b)\s+(\B)|(\B)\s+(\b)/gm, '')
        .trim();
}

function handleAuthenticationErrors<T>(data: GraphqlResponse<T>): GraphqlResponse<T> {
    if (data.errors
        && data.errors.findIndex(e => e.extensions.classification === UNAUTHORIZED) !== -1) {
        notificationService.pushNotification({
            code: NotificationCode.UNAUTHORIZED,
            severity: Severity.error,
            message: 'You are not login on !'
        });
        throw new UnauthorizedError('You are not login on !');
    } else {
        return data;
    }
}

function handleSyntaxErrors<T>(data: GraphqlResponse<T>): GraphqlResponse<T> {
    if (data.errors
        && data.errors.findIndex(e => e.extensions.classification === INVALID_SYNTAX) !== -1) {
        notificationService.pushNotification({
            code: NotificationCode.ERROR,
            severity: Severity.error,
            message: 'Application fail to fetch server !'
        });
        throw new UnknownFetchError('Application fail to fetch server !');
    } else {
        return data;
    }
}

function handleInternalServerErrors<T>(data: GraphqlResponse<T>): GraphqlResponse<T> {
    if (data.errors
        && data.errors.findIndex(e => e.extensions.classification === INTERNAL_ERROR) !== -1) {
        notificationService.pushNotification({
            code: NotificationCode.ERROR,
            severity: Severity.error,
            message: 'An error occurred on the server side'
        });
        throw new UnknownFetchError('An error occurred on the server side !');
    } else {
        return data;
    }
}

export function send<T>(query: string, vars?: any): Observable<GraphqlResponse<T>> {
    const headers = new Headers();
    headers.set(ConstantHttpHeaders.CONTENT_TYPE, ConstantMediaTypes.JSON_UTF8);
    return fromFetch(baseUrl, {
        method: 'POST',
        headers: headers,
        body: JSON.stringify({
            query: gqlMinify(query),
            variables: vars
        })
    }).pipe(
        switchMap(handleStatusCodeErrors),
        switchMap(r => r.json()),
        map(handleAuthenticationErrors),
        map(handleSyntaxErrors),
        map(handleInternalServerErrors),
    );
}
