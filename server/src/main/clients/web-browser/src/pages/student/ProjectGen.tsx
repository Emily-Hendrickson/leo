import React from 'react';
import Venn, {stringSet} from '../../utils/Venn';

function App() {
  const style: React.CSSProperties = {
    display: 'flex',
    flexFlow: 'column nowrap',
    justifyContent: 'center',
    alignItems: 'center',
  };

  // Unused variable input will be used in API call in future
  //eslint-disable-next-line @typescript-eslint/no-unused-vars
  const processInput = async (input: stringSet): Promise<stringSet> => {
    return [
      'Exponential Function in Video Games: Have students analyze the way exponential functions are used in video games, such as in scoring systems or character growth. They can then create their own video game using exponential functions, incorporating programming skills to bring the game to life.',
      'Engineering Design Challenge: Have students design and build a system using exponential functions, such as a solar panel array or a machine that performs a task exponentially faster over time. Students can use programming to control and monitor the performance of their system.',
      'Function Transformations in Physics: Have students explore how exponential functions can model real-world phenomena, such as radioactive decay or the spread of a disease. They can then use function transformations and programming skills to analyze and predict these processes.',
      'Cost-Benefit Analysis for Business: Have students use exponential functions to model the costs and benefits of a new product or technology. They can then use programming skills to create interactive visualizations or simulations to communicate their findings.',
    ];
  };

  return (
    <main style={style}>
      <Venn
        processInput={processInput}
        options={[
          ['Video Games', 'Sports', 'Music', 'Electronics'],
          [
            'EKS 1 - Linear Functions',
            'EKS 2 - Exponential Functions',
            'EKS 3 - Logarithmic Functions',
            'EKS 4 - Rational Functions',
          ],
          ['Computer Engineer', 'Data Scientist', 'Computer Programmer'],
          [
            'Data specialists pro sports',
            'Programming',
            'Storytelling with data',
          ],
        ]}
        pageURI="/student/project-implement"
      />
    </main>
  );
}

export default App;