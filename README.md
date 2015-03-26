# Clojure-NLP

This is a Clojure project that explores the capabilities of the OpenNLP Java Library via the clojure-opennlp library.
It's purpose is to translate a request to schedule a calendar event - an appointment or meeting - from plain text
into some form of structured data.

## Usage

From the REPL, with Leiningen:

    $ lein repl

    nlp.core=>  (parse-messsage "Schedule an appointment with Susan Rice on July 22 2015 at 3:15PM to discuss foreign affairs")

## More Documentation

These blog entries describe the code in-depth and includes many examples.

[From Natural Language to Calendar Entries, with Clojure] (http://edeferia.blogspot.com/2015/03/from-natural-language-to-calendar.html)

[From Natural Language to Calendar Entries with Clojure - Part 2] (http://edeferia.blogspot.com/2015/03/from-natural-language-to-calendar_15.html)

[From Natural Language to Calendar Entries with Clojure - Part 3]
(http://edeferia.blogspot.com/2015/03/from-natural-language-to-calendar_26.html)

### Known Issues

As of March 15, 2015: The date finder model is not robust enough to handle all typical date format. More specifically, the time
component of a datetime can only be parsed if the hours are expessed in HH format - 2 digits. Thus, instead of 2:00PM, one would
need 02:00PM.


## License

Copyright © 2015 Ernie de Feria

Distributed under the Eclipse Public License - v 1.0. See file COPYING.
