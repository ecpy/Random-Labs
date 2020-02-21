package Car

type Car struct {
	engine engine
}

func New() *Car {
	return &Car{engine: engine.New()}
}
