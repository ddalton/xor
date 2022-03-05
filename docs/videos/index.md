---
layout: page
title: Videos
permalink: /videos/
---

Under construction.

## Featured
{% assign random = site.time | date: "%s%N" | modulo: site.data.xor-videos.size %}
{% assign featured = site.data.xor-videos[random] %}

{{ featured.topic }} - [{{ featured.speaker }}](https://twitter.com/{{ featured.twitter_handle }})
<div class="videoWrapper">
    <iframe width="420" height="315" src="https://www.youtube.com/embed/{{ featured.youtube_id }}" frameborder="0" allowfullscreen></iframe>
</div>

{% assign talks = site.data.xor-videos | group_by: 'year' %}
{% for year in talks reversed %}
## {{ year.name }}
    {% for talk in year.items %}
 * [{{ talk.topic }}](https://youtu.be/{{ talk.youtube_id }}) - [{{ talk.speaker }}](https://twitter.com/{{ talk.twitter_handle }})
    {% endfor %}
{% endfor %}
