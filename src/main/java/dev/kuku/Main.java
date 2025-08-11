package dev.kuku;

public class Main {
    private static class RoadMap {
        //1. Publisher Consumer Support
        //2. Different LEVELS for logs

        public void PublisherConsumerSupport() {
            /*
            Event listeners will be an annotation and it will scan the methods for event block data

            Or They are annotated with SubBlock but we modify SubBlock to check if it contains EventPublissherData.
            So we can use data to create event listener

            What about starting starting publisher. The only way is to explicitly

            So we need to have an explicit VFL Starter and SubBlock will can be used inside it
             */

            //TODO RootBlock annotation
            //TODO ensure the bfl buffer flush makes sense for all impls
            //TODO fluent in future
            //todo service to service block creation
        }
    }
}