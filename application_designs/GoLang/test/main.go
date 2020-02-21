package main

import (
	"fmt"

	"sync"

	"golang.org/x/sync/semaphore"
)

func testChannel(wg *sync.WaitGroup) {

	const bufferSize = 100
	ch := make(chan int, bufferSize)

	go func() {
		i := 0
		for {
			ch <- i
			i += 1
		}
	}()

	go func() {
		for {

			if i := <-ch; i == 100 {
				break
			}
		}
	}()

	wg.Done()
}

func testSemaphore(wg *sync.WaitGroup) {

	sp := semaphore.NewWeighted(2)

	if sp.TryAcquire(3) {
		fmt.Println("TryAcquire ok")
	} else {
		fmt.Println("TryAcquire not ok")
	}

	wg.Done()

}

func testError() (err string) {

	defer func() {
		named = "I return a error"
		fmt.Println(named)
		// fmt.Println("this is an err")
	}()

	panic("oh! panic")

	return
}

func testDataTypes(wg *sync.WaitGroup) {
	// array
	arr := [4]interface{}{0, 1, 2, 3}
	fmt.Printf("testDataTypes: arr: %t\n", arr)

	// array slice
	arrSlice := arr[1:2]
	fmt.Printf("testDataTypes: %t\n", arrSlice)
	fmt.Println("testDataTypes: array slice", arrSlice)

	// new slice
	sliceSize := 10
	slice := make([]interface{}, sliceSize)
	fmt.Printf("testDataTypes: new slice: %t\n", slice)

	// map
	m := make(map[*int]*int)
	fmt.Printf("testDataTypes: map: %t\n", m)

}

func testGoModule(wg *sync.WaitGroup) {

	wg.Done()
}

func main() {

	wg := &sync.WaitGroup{}
	wg.Add(5)

	go testChannel(wg)
	go testSemaphore(wg)
	go testGoModule(wg)
	go testDataTypes(wg)

	// test waitgroup
	wg.Wait()

	// test err
	testError()

}
