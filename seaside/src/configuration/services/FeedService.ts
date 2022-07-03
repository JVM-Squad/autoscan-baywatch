import {map, switchMap, take} from "rxjs/operators";
import {HttpStatusError} from "@/common/errors/HttpStatusError";
import {Feed} from "@/configuration/model/Feed.type";
import {Page} from "@/services/model/Page";
import {from, Observable, throwError} from "rxjs";
import {ConstantFilters, ConstantHttpHeaders} from "@/constants";
import rest from '@/common/services/RestWrapper';
import {OpPatch} from "json-patch";
import gql from "@/common/services/GraphqlWrapper";
import {AtomFeed, ScrapFeedHeaderResponse} from "@/configuration/model/GraphQLScraper.type";

export const URL_PATTERN = new RegExp('^(https?:\\/\\/)?' + // protocol
    '((([a-z\\d]([a-z\\d-]*[a-z\\d])*)\\.)+[a-z]{2,}|' + // domain name
    '((\\d{1,3}\\.){3}\\d{1,3}))' + // OR ip (v4) address
    '(\\:\\d+)?(\\/[-a-z\\d%_.~+]*)*' + // port and path
    '(\\?[;&a-z\\d%_.~+=-]*)?' + // query string
    '(\\#[-a-z\\d_]*)?$', 'i'); // fragment locator

export class FeedService {

    public static readonly DEFAULT_PER_PAGE: number = 20;
    public static readonly DEFAULT_QUERY: string = `?${ConstantFilters.PER_PAGE}=${FeedService.DEFAULT_PER_PAGE}&_s=name`;

    private static readonly SCRAP_FEED_HEAD_REQUEST = `#graphql
    query ScrapFeedHeader($link: URI!) {
        scrapFeedHeader(link: $link) {
            title
            description
        }
    }`;

    /**
     * Get the {@link Feed} from backend
     *
     * @param page The to display
     * @param query The possible query parameters
     */
    public list(page = 0, query: URLSearchParams = new URLSearchParams(FeedService.DEFAULT_QUERY)): Observable<Page<Feed>> {
        const resolvedPage = (page > 0) ? page : 0;
        query.set(ConstantFilters.PAGE, String(resolvedPage));
        let resolvedPerPage = query.get(ConstantFilters.PER_PAGE);
        if (resolvedPerPage === null) {
            resolvedPerPage = String(FeedService.DEFAULT_PER_PAGE);
            query.append(ConstantFilters.PER_PAGE, resolvedPerPage);
        }

        return rest.get(`/feeds?${query.toString()}`).pipe(
            map(response => {
                if (response.ok) {
                    const totalCount = parseInt(response.headers.get(ConstantHttpHeaders.X_TOTAL_COUNT) || "-1");
                    const data: Observable<Feed[]> = from(response.json());
                    return {
                        currentPage: resolvedPage,
                        totalPage: Math.ceil(totalCount / Number(resolvedPerPage)),
                        data: data
                    };
                } else {
                    throw new HttpStatusError(response.status, `Error while getting news.`);
                }
            }),
            take(1)
        );
    }

    public add(feed: Feed): Observable<Feed> {
        return rest.post('/feeds', feed).pipe(
            switchMap(this.responseToFeed),
            take(1)
        );
    }

    public update(feed: Feed, urlChange: boolean = true): Observable<string> {
        if (!urlChange) {
            return rest.put(`/feeds/${feed.id}`, feed).pipe(
                switchMap(this.responseToFeed),
                map((updatedFeed: Feed) => updatedFeed.id),
                take(1)
            );
        } else {
            const jsonPatch: OpPatch[] = [];
            jsonPatch.push({op: 'remove', path: `/feeds/${feed.id}`});
            jsonPatch.push({op: 'add', path: '/feeds', value: feed});

            return this.patch(jsonPatch).pipe(
                map(updated => updated.pop())
            );
        }
    }

    public remove(id: string): Observable<Feed> {
        return rest.delete(`/feeds/${id}`).pipe(
            switchMap(this.responseToFeed),
            take(1)
        );
    }

    public bulkRemove(ids: string[]): Observable<number> {
        const jsonPatch: OpPatch[] = [];
        ids.forEach(id => jsonPatch.push({op: 'remove', path: `/feeds/${id}`}));
        return this.patch(jsonPatch).pipe(
            map(deleted => deleted.length)
        );
    }

    private patch(payload: OpPatch[]): Observable<string[]> {
        return rest.patch('/feeds', payload).pipe(
            switchMap(this.responseToFeed),
            take(1)
        );
    }

    private responseToFeed(response: Response): Observable<any> {
        if (response.ok) {
            return from(response.json());
        } else {
            return from(response.json()).pipe(switchMap(j =>
                throwError(() => new HttpStatusError(response.status, j.message))));
        }
    }

    public fetchFeedInformation(link: string): Observable<Feed> {
        if (link === undefined) {
            return throwError(() => new Error('Link is mandatory !'));
        } else if (!URL_PATTERN.test(link)) {
            return throwError(() => new Error('Argument link must be a valid URL !'));
        }

        return gql.send<ScrapFeedHeaderResponse>(FeedService.SCRAP_FEED_HEAD_REQUEST, {link: link}).pipe(
            map(data => data.data.scrapFeedHeader),
            map((atom: AtomFeed) => ({
                name: atom.title,
                description: atom.description,
            } as Feed)),
            take(1),
        );
    }
}

export default new FeedService();