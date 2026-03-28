package com.main;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class MonoFluxtest {


    @Test
    public void testMonoFlux() throws InterruptedException {


        Mono<String> monoString = Mono.just("arjun_dhatbale");
        monoString.subscribe(System.out::println);


        Mono<Integer> monoNUmber = Mono.just(3);
        monoNUmber.map(n -> n * n)
                .subscribe(result ->
                        System.out.println("square of no : " + result));
    }


    @Test
    public void filterData(){
        Flux<Integer> flux = Flux.just(1,3,4,5,6,8,0);

        flux.filter(no -> no > 3)
                .subscribe(result ->
                        System.out.println("result = " + result));
    }


    @Test
    public void combiningResult(){
        Mono<Integer> age = Mono.just(27);
        Mono<String> name = Mono.just("Arjun");

        Mono.zip(name, age)
                .subscribe(result  ->
                        System.out.println("Result name " + result.getT1() + " and age : " + result.getT2()));
    }

    @Test
    public void errorHanding(){

        Mono<Integer> number = Mono.just(10)
                .map(no -> no / 0);

        number.onErrorResume(e -> {
            System.out.println("Error occurred : " + e.getLocalizedMessage());
            return Mono.just(0);
        })
                .subscribe(result -> System.out.print("Result :  " + result));
    }
}
