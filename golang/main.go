package main

import (
	"fmt"
	"math/rand"
	"sync"
	"time"
)

type ship struct {
	toUnload int
	toUpload int
}

type SafeContainers struct {
	m     sync.Mutex
	count int
}

type SafeQueue struct {
	m       sync.Mutex
	ships   []ship
	current int
}

var (
	berthsCount   = 3
	maxContainers = 40
	containers    = SafeContainers{count: 0}
	queue         = SafeQueue{current: 0, ships: []ship{
		{0, 5},
		{0, 2},
		{10, 0},
		{4, 0},
		{2, 10},
		{2, 0},
	}}
	wg sync.WaitGroup
)

func berth() {
	for {
		queue.m.Lock()
		if queue.current == len(queue.ships) {
			queue.m.Unlock()
			break
		}
		ship := queue.ships[queue.current]
		queue.current += 1
		queue.m.Unlock()

		comeDelay := 500 + rand.Intn(1000)
		time.Sleep(time.Duration(comeDelay) * time.Millisecond)

		for {
			containers.m.Lock()
			if containers.count+ship.toUnload <= maxContainers {
				containers.count += ship.toUnload
				containers.m.Unlock()
				break
			} else {
				fmt.Printf("Waiting to unload %d containers, current: %d\n", ship.toUnload, containers.count)
			}
			containers.m.Unlock()
			time.Sleep(1 * time.Second)
		}

		for {
			containers.m.Lock()
			if containers.count-ship.toUpload >= 0 {
				containers.count -= ship.toUpload
				containers.m.Unlock()
				break
			} else {
				fmt.Printf("Waiting to upload %d containers, current: %d\n", ship.toUpload, containers.count)
			}
			containers.m.Unlock()
			time.Sleep(1 * time.Second)
		}

		leaveDelay := 1500 + rand.Intn(2000)
		time.Sleep(time.Duration(leaveDelay) * time.Millisecond)
		fmt.Printf("Ship served: %d, %d\n", ship.toUnload, ship.toUpload)
	}
	wg.Done()
}

func main() {
	wg.Add(berthsCount)
	for i := 0; i < berthsCount; i++ {
		go berth()
	}
	wg.Wait()
	fmt.Printf("All ships served. Port has %d containers\n", containers.count)
}
