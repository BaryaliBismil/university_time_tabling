import java.util.Date;
import java.util.Iterator;

/**
 * Created by Martin on 05-04-2015.
 */
public abstract class Heuristic {
    /**
     * Perform a search with the input schedule as a starting point. The method will return the best schedule
     * found before timeout.
     * This method is abstract and must be implemented according to the chosen heuristic.
     * @param schedule The Schedule to start from
     * @return The most optimal schedule found during search.
     */
    public abstract Schedule search(Schedule schedule);

    private int timeout = 300;

    /**
     * @return the duration, in seconds, that the heuristic is allowed to search for solutions.
     */
    public int getTimeout() {
        return timeout;
    }

    /**
     * Sets the duration, in seconds, that the heuristic is allowed to search for solutions.
     * @param seconds the duration in seconds. Must be a positive value.
     */
    public void setTimeout(int seconds) {
        if (seconds <= 0)
            return;
        timeout = seconds;
    }

    /**
     * The millisecond time when the countdown was started.
     */
    private long countdownStartTime;

    /**
     * Starts the countdown timer. Call timeoutReached() to determine when the timeout has been reached.
     */
    protected void startCountdown() {
        countdownStartTime = new Date().getTime();
    }

    /**
     * Returns a value indicating whether the timeout has currently been reached.
     * startCountdown() must be called prior to calling this method.
     * @return true if the timeout has been reached, or if the countdown has not yet started. If not, false.
     */
    protected boolean timeoutReached() {
        Date currentTime = new Date();
        long delta = (currentTime.getTime() - countdownStartTime) / 1000;
        return delta > timeout;
    }

    /**
     * Checks that courses with the same lecturer is not assigned in same time slots
     * @return true if the constraint is satisfied
     */
    public boolean validateSameLecturerConstraint(Schedule schedule) {
        // For a given day, keep track of which lecturers are busy
        boolean[] lecturerBusy;
        for (int day = 0; day < basicInfo.days; day++) {
            for (int period = 0; period < basicInfo.periodsPerDay; period++) {
                // Assume all lecturers are idle
                lecturerBusy = new boolean[basicInfo.lecturers];

                for (int room = 0; room < basicInfo.rooms; room++) {
                    // Check which course, if any, is assigned to this room at this time
                    int assignedCourse = schedule.assignments[day][period][room];
                    if (assignedCourse == -1)
                        continue;

                    // Remember that the lecturer is busy. If we already know this, the constraint is violated.
                    int lecturer = courses.lecturerForCourse[assignedCourse];
                    if (lecturerBusy[lecturer])
                        return false;
                    lecturerBusy[lecturer] = true;
                }
            }
        }

        return true;
    }

    public BasicInfo basicInfo;
    public Curriculum curriculum;
    public Lecturers lecturers;
    public Courses courses;
    public Unavailability unavailability;
    public Rooms rooms;

    /**
     * Checks that courses in the same curriculum are not scheduled in the same time slots
     * @return true if the constraint is satisfied
     */
    public boolean validateSameCurriculumConstraint(Schedule schedule) {
        // For a given day, keep track of which lecturers are busy
        boolean[] curriculumBusy;
        for (int day = 0; day < basicInfo.days; day++) {
            for (int period = 0; period < basicInfo.periodsPerDay; period++) {
                // Assume all curricula are idle
                curriculumBusy = new boolean[basicInfo.curricula];

                for (int room = 0; room < basicInfo.rooms; room++) {
                    // Check which course, if any, is assigned to this room at this time
                    int assignedCourse = schedule.assignments[day][period][room];
                    if (assignedCourse == -1)
                        continue;

                    for (int curriculum = 0; curriculum < basicInfo.curricula; curriculum++) {
                        if (this.curriculum.isCourseInCurriculum[assignedCourse][curriculum]) {
                            if (curriculumBusy[curriculum])
                                return false;
                            curriculumBusy[curriculum] = true;
                        }
                    }
                }
            }
        }
        return true;
    }

    /**
     * Checks that courses are not scheduled in unavailable time slots
     * @return true if the constraint is satisfied
     */
    public boolean validateAvailabilityConstraint(Schedule schedule) {
        // Iterate through all constraints and check whether any have been violated
        Iterator<UnavailabilityConstraint> iter = unavailability.constraints.iterator();
        while (iter.hasNext()) {
            UnavailabilityConstraint c = iter.next();

            for (int room = 0; room < basicInfo.rooms; room++) {
                if (schedule.assignments[c.day][c.period][room] == c.course)
                    return false;
            }
        }
        return true;
    }

    public Schedule getRandomInitialSolution(){
        Schedule result = new Schedule(basicInfo.days, basicInfo.periodsPerDay, basicInfo.rooms);
        int[] courseAssignmentCount = new int[basicInfo.courses];
        
        for (int day = 0; day < basicInfo.days; day++) {
    		boolean[] courseAlreadyAssigned = new boolean[basicInfo.courses];
    		
        	for (int period = 0; period < basicInfo.periodsPerDay; period++) {
        		boolean[] lecturerBusy = new boolean[basicInfo.lecturers];
        		boolean[] curriculumBusy = new boolean[basicInfo.curricula];
        		for (int room = 0; room < basicInfo.rooms; room++) {
        			int assignedCourse = -1; // fix
        			int candidatecourse = -1;
        			// find the course to assign, subject to
        			while (assignedCourse == -1) {
        				candidatecourse++; // maybe use a priority queue instead?
        				
        				if (candidatecourse == basicInfo.courses)
        					break;
        				
	        			// course not already assigned in time slot
        				if (courseAlreadyAssigned[candidatecourse])
        					continue;
        				
	        			// course not unavailable in time slot
        				if (unavailability.courseUnavailable[day][period][candidatecourse])
        					continue;
        				
	        			// course lecturer cannot be busy
	        			if (lecturerBusy[courses.lecturerForCourse[candidatecourse]])
	        				continue;
        				
        				// course curriculum cannot be busy
	        			boolean curriculumConflict = false;
	                    for (int curriculum = 0; curriculum < basicInfo.curricula; curriculum++) {
	                        if (this.curriculum.isCourseInCurriculum[candidatecourse][curriculum]) {
	                            if (curriculumBusy[curriculum])
	                                curriculumConflict = true;
	                        }
	                    }
	                    if (curriculumConflict)
	                    	continue;
	        			
	        			// course max lectures cannot be reached
	                    if (courseAssignmentCount[candidatecourse] == courses.numberOfLecturesForCourse[candidatecourse])
	                    	continue;
	                    
	        			// course minimum working days .... ?
	                    
	                    // no constraints violated! assign this course
	                    assignedCourse = candidatecourse;
        			}
        			
        			if (assignedCourse == -1)
        				continue;
        			
        			// increment constraints
        			courseAlreadyAssigned[assignedCourse] = true;
        			lecturerBusy[courses.lecturerForCourse[candidatecourse]] = true;
        			for (int curriculum = 0; curriculum < basicInfo.curricula; curriculum++) {
                        if (this.curriculum.isCourseInCurriculum[assignedCourse][curriculum]) {
                            curriculumBusy[curriculum] = true;
                        }
                    }
        			courseAssignmentCount[candidatecourse]++;
        			
        			result.assignments[day][period][room] = assignedCourse;
        		}
        	}
        }
        
        return result;
        }
    
 public int evaluationFunction(Schedule schedule){
    	
    	int[] numberOfLecturesOfCourse = new int[basicInfo.courses];
    	
    	for(int i=0; i < basicInfo.courses; i++){
    		numberOfLecturesOfCourse[i] = courses.numberOfLecturesForCourse[i];
    	}
    	
    	//to calculate the number of unallocated lectures of each course
    	for(int day= 0; day < basicInfo.days; day++){
    		for(int period = 0; period < basicInfo.periodsPerDay; period++){
    			for(int room = 0; room < basicInfo.rooms; room++){
    				int assignedCourse = schedule.assignments[day][period][room];
    				if(assignedCourse == -1)
    					continue;
    				numberOfLecturesOfCourse[assignedCourse]--;
    			}
    		}
    	}
    	
    	int unscheduled;
    	for(int i=0; i <basicInfo.courses;i++){
    		
    	}
    	
    	//to calculate the number of days of each course that is scheduled below the minimum number of working days
    	int[] minimumWorkingDaysOfCourse = new int[basicInfo.courses];
    	
    	for(int i=0; i < basicInfo.courses; i++){
    		minimumWorkingDaysOfCourse[i] = courses.minimumWorkingDaysForCourse[i];
    	}
    	
    	for(int day = 0; day < basicInfo.days; day++){
    		// to avoid overcount working days of each course
    		boolean[] dayFulfilled = new boolean[basicInfo.courses];
    		for(int i = 0; i < basicInfo.courses; i++){
    			dayFulfilled[i] = false;
    		}
    		
    		for(int period = 0; period < basicInfo.periodsPerDay; period++){
    			for(int room = 0; room < basicInfo.rooms; room++){
    				int assignedCourse = schedule.assignments[day][period][room];
    				if(assignedCourse == -1)
    					continue;
    				if(dayFulfilled[assignedCourse] == true)
    					continue;
    				dayFulfilled[assignedCourse] = true;
    				minimumWorkingDaysOfCourse[assignedCourse]--;
    			}
    		}
    	}
    	
    	int[][][] secludedLecture = new int[basicInfo.days][basicInfo.periodsPerDay][basicInfo.curricula];
    	
    	//Initialise the value of each slot in secludedLecture, 1 if a curriculum in a timeslot has a secluded lecture
    	for(int day = 0; day < basicInfo.days; day++){
    		for(int period = 0; period < basicInfo.periodsPerDay; period++){
    			for(int curriculum = 0; curriculum < basicInfo.curricula; curriculum++){
    				secludedLecture[day][period][curriculum] = 0;
    			}
    		}
    	}
    	
    	//to calculate the number of secluded lectures of each curriculum
    	for(int day = 0; day < basicInfo.days; day++){
    		for(int period = 0; period < basicInfo.periodsPerDay; period++){
    			for(int curriculum = 0; curriculum < basicInfo.curricula; curriculum++){
    				int count1 = 0; // to calculate X_c,t,r
    				int count2 = 0; // to calculate X_c',t',r'
    				for(int course = 0; course < basicInfo.courses; course++){
    					if (this.curriculum.isCourseInCurriculum[course][curriculum] == true){
    						for(int room = 0; room < basicInfo.rooms; room++){
    	    					if(schedule.assignments[day][period][room] == course)
    	    						count1++;
    	    				}
        				}
    				}
    				int adjacentPeriod1 = period - 1;
    				int adjacentPeriod2 = period + 1;
    				
    				if(adjacentPeriod1 >= 0){
    					for(int course = 0;course < basicInfo.courses; course++){
    						if(this.curriculum.isCourseInCurriculum[course][curriculum] == true){
    							for(int room = 0; room < basicInfo.rooms;room++){
    								if(schedule.assignments[day][adjacentPeriod1][room] == course)
    									count2++;
    							}
    						}
    					}
    				}
    				
    				if(adjacentPeriod2 < basicInfo.periodsPerDay){
    					for(int course = 0;course < basicInfo.courses; course++){
    						if(this.curriculum.isCourseInCurriculum[course][curriculum] == true){
    							for(int room = 0; room < basicInfo.rooms;room++){
    								if(schedule.assignments[day][adjacentPeriod2][room] == course)
    									count2++;	
    							}
    						}
    					}
    				}
    				
    				if(count1 == 1 && count2 == 0)
    					secludedLecture[day][period][curriculum] = 1;
    			}
    		}
    	}
    	
    	//to calculate number of room changes of each courses
    	int[] numberOfRoomChanges = new int[basicInfo.courses];
    	
    	//if the course is always taught in same room, the value is 0
    	//if the course is never allocated, the value is -1
    	for(int course = 0; course < basicInfo.courses; course++){
    		numberOfRoomChanges[course] = -1;
    	}
    	
    	for(int course = 0; course < basicInfo.courses; course++){ 
    		boolean[] roomChanged = new boolean[basicInfo.rooms];
        	
        	for(int room = 0; room < basicInfo.rooms; room++){
        		roomChanged[room] = false;
        	}
        	
    		for(int day = 0; day < basicInfo.days; day++){
    			for(int period = 0; period < basicInfo.periodsPerDay; period++){
        			for(int room = 0; room < basicInfo.rooms; room++){
        				if(schedule.assignments[day][period][room] == course)
        					roomChanged[room] = true;
        			}
    			}
    		}
    		
    		for(int room = 0; room < basicInfo.rooms; room++){
        		if(roomChanged[room] == true)
        			numberOfRoomChanges[course]++;
        	}	
    	}
    	
    	//to calculate the amount of capacity that room is exceeded in a timeslot
    	int leftOverCapacity = 0;
    	
    	for(int day = 0; day < basicInfo.days; day++){
    		for(int period = 0; period < basicInfo.periodsPerDay; period++){
    			for(int room = 0; room < basicInfo.rooms; room++){
    				
    				int course = schedule.assignments[day][period][room];
    				if(course != -1){
    					if(this.rooms.capacityForRoom[room] < this.courses.numberOfStudentsForCourse[course])
        					leftOverCapacity += (this.courses.numberOfStudentsForCourse[course] - this.rooms.capacityForRoom[room]);
        			}
    			}		
    		}
    	}
    	
    	int objective = 0; // calculate the penalties 
    	
    	for(int i = 0; i < basicInfo.courses; i++){
    		objective += 10*numberOfLecturesOfCourse[i];
    	}
    	
    	for(int course = 0; course < basicInfo.courses; course++){
    		objective += 5*minimumWorkingDaysOfCourse[course];
    	}
    	
    	for(int day = 0; day < basicInfo.days; day++){
    		for(int period = 0; period < basicInfo.periodsPerDay; period++){
    			for(int curriculum = 0; curriculum < basicInfo.curricula; curriculum++){
    				objective += 2*secludedLecture[day][period][curriculum];
    			}
    		}
    	}
    	
    	for(int course = 0; course < basicInfo.courses; course++){
    		objective += numberOfRoomChanges[course];
    	}
    	
    	objective += leftOverCapacity;
    	
    	return objective;
    }
    
	 	//[day][period][room]
 	protected void DeepClone(Schedule original,Schedule copy) {
 		for(int day=0;day<original.assignments.length;day++) {
 			for(int period =  0;period<original.assignments[day].length;period++) {
 				for(int room = 0;room<original.assignments[day][period].length;room++) {
 					//System.out.println("day = " + day + " period = "+period + " room = "+room);
 					copy.assignments[day][period][room] = original.assignments[day][period][room];
 				}
 			}
 		}
		
	}
 	
 	protected void swapCourse (int day1,int period1 ,int room1,int day2,int period2 ,int room2 , Schedule Content ) {
 		
 		int temp = Content.assignments[day1][period1][room1]; 
 		Content.assignments[day1][period1][room1] = Content.assignments[day2][period2][room2];
 		Content.assignments[day2][period2][room2] = temp;
 	}
}
