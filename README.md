Re: https://twitter.com/craiger/status/790714001294360576

```
$ sbt run
[info] Running Example
List(select x2."id", x2."distribution_id", x2."transcriber_id", x2."start", x2."duration", x2."expiry", x2."state", x2."comment", x2."chunk_num", x3."thumbnail_url", x4."chunk_id" from (select "id" as x5, "email" as x6 from "user" where "id" = '7c43d263-9587-46ce-99d4-551b3ae766b3') x7 left outer join "chunk" x2 inner join "distribution" x8 inner join "content" x3 on x3."id" = x8."content_id" on x8."id" = x2."distribution_id" on x7.x5 = x2."transcriber_id" left outer join "subtitles" x4 on x2."id" = x4."chunk_id")
[success] Total time: 12 s, completed 25-Oct-2016 12:12:43
```

